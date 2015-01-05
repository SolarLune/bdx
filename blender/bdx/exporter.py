import os
import re
import sys
import json
import math
import pprint
import shutil
import bpy
import mathutils as mt
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, EnumProperty, BoolProperty
from bpy.types import Operator
from . import utils as ut


def poly_indices(poly):
    if len(poly.vertices) < 4:
        return list(poly.vertices)

    return [poly.vertices[i] for i in (0, 1, 2, 2, 3, 0)]

def triform(loop_indices):
    indices = list(loop_indices)

    if len(indices) < 4:
        return indices
    
    return [indices[i] for i in (0, 1, 2, 2, 3, 0)]

class EmptyUV:
    uv = (0.0, 0.0)
    def __getitem__(self, index):
        return self

def flip_uvs(uvs):
    for v in uvs:
        v[1] -= 0.5
        v[1] *= -1
        v[1] += 0.5
    
def vertices(mesh):
    uv_act = mesh.uv_layers.active
    uv_layer = uv_act.data if uv_act is not None else EmptyUV()

    loop_vert = {l.index: l.vertex_index for l in mesh.loops}

    verts = []

    for poly in mesh.polygons:
        poly_verts = []
        poly_uvs = []

        for li in triform(poly.loop_indices):
            poly_verts.append(list(mesh.vertices[loop_vert[li]].co))
            poly_uvs.append(mt.Vector(uv_layer[li].uv))

        flip_uvs(poly_uvs)

        for v, uv in zip(poly_verts, poly_uvs):
            verts += v + list(uv)

    return verts


def in_active_layer(obj):
    layer = [i for i, v in enumerate(obj.layers) if v][0]
    active_layers = [i for i, v in enumerate(scene.layers) if v]

    if layer in active_layers:
        return True

    return False


def instance(dupli_group):
    if dupli_group:
        return [o.name for o in dupli_group.objects if not o.parent][0]


def mat_tris(mesh):
    """Returns dict: mat_name -> list_of_triangle_indices"""

    m_ps = {}

    idx_tri = 0
    for p in mesh.polygons:
        mat = mesh.materials[p.material_index] if mesh.materials else None
        mat_name = mat.name if mat else "__BDX_DEFAULT"
        if not mat_name in m_ps:
            m_ps[mat_name] = []

        m_ps[mat_name].append(idx_tri)
        idx_tri += 1

        if len(p.loop_indices) > 3:
            m_ps[mat_name].append(idx_tri)
            idx_tri += 1

    return m_ps


def used_meshes(objects):
    return [o.data for o in objects 
            if o.type == "MESH"]


def srl_models(meshes):
    name_model = {}

    tfs = 3 * 5 # triangle float size: 3 verts at 5 floats each

    for mesh in meshes:
        m_tris = mat_tris(mesh)
        verts = vertices(mesh)
        m_verts = {}
        for m, tris in m_tris.items():
            m_verts[m] = sum([verts[i * tfs : i * tfs + tfs] for i in tris], [])
        name_model[mesh.name] = m_verts

    return name_model

def char_uvs(char, angel_code):
    """
    Return a list of uv coordinates (for a quad)
    which encompass the relevant character on the font 
    texture, as specified by the angel code format.

    """
    cm = angel_code["common"]
    W, H = cm["scaleW"], cm["scaleH"]

    try:
        c = angel_code["char"][str(ord(char))]
    except:
        c = angel_code["char"][str(ord(' '))]

    x, y = c['x'], c['y']
    w, h = c['width'], c['height']

    pu = lambda x, y: [1 / W * x, 1 / H * y]

    #u = [[0, 0],
    #     [1, 0],
    #     [1, 1],
    #     [0, 1]]

    uvs = [pu(x, y + h),
           pu(x + w, y + h),
           pu(x + w, y),
           pu(x, y)]

    #flip_uvs(uvs)

    return uvs


def vertices_text(text, angel_code):
    """Generate vertex data for a text object"""

    ac = angel_code
    o_c = ac["char"][str(ord('O'))]
    builtin = ac["info"]["face"] == "Bfont"
    scale = 0.0225 * (1.4 if builtin else 1)
    unit_height = o_c["height"] * scale

    verts = []
    pos = 0
    z = 0

    for char in text.body:
        # Make quad

        try:
            c = ac["char"][str(ord(char))]
        except:
            c = ac["char"][str(ord(' '))]

        x, y = pos + c["xoffset"], 0 - c["yoffset"]
        w, h = c["width"], c["height"]
        pos += c["xadvance"]

        q = [[x  , y-h, z],
             [x+w, y-h, z],
             [x+w, y  , z],
             [x  , y  , z]]

        z += 0.0001

        for v in q:
            v[0] *= scale
            v[1] *= scale
            v[0] -= 0.05 + (0.03 if builtin else 0)
            v[1] += unit_height * (0.76 - (0.05 if builtin else 0))

        quad = [v + uv for v, uv in zip(q, char_uvs(char, ac))]

        # To triangles
        tris = [quad[i] for i in (0, 1, 2, 2, 3, 0)]
        verts += sum(tris, [])

    return verts

def srl_models_text(texts, fntx_dir):
    j = os.path.join

    def fntx(t):
        with open(j(fntx_dir, t.font.name + ".fntx"), 'r') as f:
            data = json.load(f)
        return data

    return {"__FNT_"+t.name: 
                {"__FNT_"+t.font.name: vertices_text(t, fntx(t))} 
            for t in texts}

def srl_materials_text(fonts):
    return {"__FNT_"+f.name:
                {"texture": "__FNT_"+f.name+".png",
                 "alpha_blend": "ALPHA",
                 "color": [1, 1, 1],
                 "opacity": 1} 
         for f in fonts}

def view_plane(camd, winx, winy, xasp, yasp):
    """
    "DEAR GOD WHY??!!"

    Well, that's because blender doesn't expose the camera's projection matrix.
    So, in order to get it, we have to actually port the C code that generates it,
    using data that is actually available in the pyhton API.

    :(

    """

    #/* fields rendering */
    ycor = yasp / xasp
    use_fields = False
    if (use_fields):
      ycor *= 2

    def BKE_camera_sensor_size(p_sensor_fit, sensor_x, sensor_y):
        #/* sensor size used to fit to. for auto, sensor_x is both x and y. */
        if (p_sensor_fit == 'VERTICAL'):
            return sensor_y;

        return sensor_x;

    if (camd.type == 'ORTHO'):
      #/* orthographic camera */
      #/* scale == 1.0 means exact 1 to 1 mapping */
      pixsize = camd.ortho_scale
    else:
      #/* perspective camera */
      sensor_size = BKE_camera_sensor_size(camd.sensor_fit, camd.sensor_width, camd.sensor_height)
      pixsize = (sensor_size * camd.clip_start) / camd.lens

    #/* determine sensor fit */
    def BKE_camera_sensor_fit(p_sensor_fit, sizex, sizey):
        if (p_sensor_fit == 'AUTO'):
            if (sizex >= sizey):
                return 'HORIZONTAL'
            else:
                return 'VERTICAL'

        return p_sensor_fit

    sensor_fit = BKE_camera_sensor_fit(camd.sensor_fit, xasp * winx, yasp * winy)

    if (sensor_fit == 'HORIZONTAL'):
      viewfac = winx
    else:
      viewfac = ycor * winy

    pixsize /= viewfac

    #/* extra zoom factor */
    pixsize *= 1 #params->zoom

    #/* compute view plane:
    # * fully centered, zbuffer fills in jittered between -.5 and +.5 */
    xmin = -0.5 * winx
    ymin = -0.5 * ycor * winy
    xmax =  0.5 * winx
    ymax =  0.5 * ycor * winy

    #/* lens shift and offset */
    dx = camd.shift_x * viewfac # + winx * params->offsetx
    dy = camd.shift_y * viewfac # + winy * params->offsety

    xmin += dx
    ymin += dy
    xmax += dx
    ymax += dy

    #/* fields offset */
    #if (params->field_second):
    #    if (params->field_odd):
    #        ymin -= 0.5 * ycor
    #        ymax -= 0.5 * ycor
    #    else:
    #        ymin += 0.5 * ycor
    #        ymax += 0.5 * ycor

    #/* the window matrix is used for clipping, and not changed during OSA steps */
    #/* using an offset of +0.5 here would give clip errors on edges */
    xmin *= pixsize
    xmax *= pixsize
    ymin *= pixsize
    ymax *= pixsize

    return xmin, xmax, ymin, ymax


def projection_matrix(camd):
    r = scene.render
    left, right, bottom, top = view_plane(camd, r.resolution_x, r.resolution_y, 1, 1)

    farClip, nearClip = camd.clip_end, camd.clip_start

    Xdelta = right - left
    Ydelta = top - bottom
    Zdelta = farClip - nearClip

    mat = [[0]*4 for i in range(4)]

    if camd.type == "ORTHO":
        for i in range(4): mat[i][i] = 1; # identity
        mat[0][0] = 2 / Xdelta
        mat[3][0] = -(right + left) / Xdelta
        mat[1][1] = 2 / Ydelta
        mat[3][1] = -(top + bottom) / Ydelta
        mat[2][2] = -2 / Zdelta #/* note: negate Z	*/
        mat[3][2] = -(farClip + nearClip) / Zdelta
    else:
        mat[0][0] = nearClip * 2 / Xdelta
        mat[1][1] = nearClip * 2 / Ydelta
        mat[2][0] = (right + left) / Xdelta #/* note: negate Z	*/
        mat[2][1] = (top + bottom) / Ydelta
        mat[2][2] = -(farClip + nearClip) / Zdelta
        mat[2][3] = -1
        mat[3][2] = (-2 * nearClip * farClip) / Zdelta

    return sum([c for c in mat], [])



def srl_objects(objects):
    name_object = {}

    def static(obj):
        return obj.game.physics_type in ("STATIC", "SENSOR")

    def bounds_type(obj):
        t = obj.game.collision_bounds_type
        if static(obj):
            if not obj.game.use_collision_bounds:
                t = "TRIANGLE_MESH"
        elif t == "TRIANGLE_MESH":
            t = "BOX"
        return t

    for obj in objects:
        matrix = obj.matrix_world

        if obj.type == "MESH":
            mesh_name = obj.data.name
        elif obj.type == "FONT":
            mesh_name = "__FNT_"+obj.data.name
        else:
            mesh_name = None

        transform = sum([list(v) for v in matrix.col], [])

        name_object[obj.name] = {
            "type": obj.type,
            "transform": transform,
            "parent": obj.parent.name if obj.parent else None,
            "model": mesh_name,
            "active": in_active_layer(obj),
            "visible": not obj.hide_render,
            "instance": instance(obj.dupli_group),
            "physics": {
                "body": obj.game.physics_type,
                "bounds": bounds_type(obj),
                "mass": 0 if static(obj) else obj.game.mass,
                "friction": obj.active_material.physics.friction if obj.active_material else 0.5,
                "restitution": obj.active_material.physics.elasticity if obj.active_material else 0,
                "ghost": obj.game.use_ghost
            }
        }


        d = name_object[obj.name]

        if obj.type == 'CAMERA':
            d["camera"] = {"projection": projection_matrix(obj.data),
                           "type": obj.data.type}
        elif obj.type == "FONT":
            d["font"] = obj.data.font.name
            d["text"] = obj.data.body

    return name_object


def used_materials(objects):
    return sum([[m for m in o.data.materials if m] for o in objects 
                if o.type == "MESH"], [])

def srl_materials(materials):
    def texture_name(m):
        if m and m.active_texture and hasattr(m.active_texture, "image"):
            return m.active_texture.image.name
        return None

    return {m.name: 
                {"texture": texture_name(m),
                 "alpha_blend": "ALPHA" if m.use_transparency else "OPAQUE",
                 "color": list(m.diffuse_color),
                 "opacity": m.alpha}
            for m in materials}


def camera_names(scene):
    return [scene.camera.name] + [o.name for o in scene.objects 
                    if o.type == "CAMERA" 
                    and o.name != scene.camera.name]


def instantiator(objects):
    """
    Returns list of java source lines, which encode an instantiator that
    binds classes in the root package to exported objects with the same name.

    """

    class_names = {f.split('.')[0] 
                   for f in os.listdir(ut.src_root()) 
                   if '.' in f}

    object_names = {o.name for o in objects}

    shared_names = class_names & object_names

    if not shared_names:
        return None

    j = os.path.join

    with open(j(ut.gen_root(), "Instantiator.java"), 'r') as f:
        lines = f.readlines()

    package_name = ut.package_name()
    lines[0] = "package " + package_name + ".inst;\n"
    lines[4] = "import " + package_name + ".*;\n"

    top = lines[:10]
    equals, new = lines[10:12]
    bottom = lines[12:]

    body = []
    for name in shared_names:
        body += [equals.replace("NAME", name),
                 new.replace("NAME", package_name + "." + name)]

    new_lines = top + body + bottom

    return new_lines


def srl_actions(actions):
    relevant = {"location":0, "rotation_euler":3, "scale":6}

    index = lambda c: relevant[c.data_path] + c.array_index

    srl_keyframe = lambda kf: [list(p) 
                               for p in (kf.handle_left, kf.co, kf.handle_right)]
    return {a.name: 
                {index(c): 
                    [srl_keyframe(kf)
                     for kf in c.keyframe_points]
                 for c in a.fcurves if c.data_path in relevant}
            for a in actions}


def used_fonts(texts):
    return {t.font for t in texts}

def texts(objects):
    return [o.data for o in objects if o.type == "FONT"]

def generate_bitmap_fonts(fonts, hiero_dir, fonts_dir, textures_dir):
    j = os.path.join

    # list of fonts to export
    existing = os.listdir(fonts_dir)
    fonts_to_export = [f for f in fonts if f.name + ".fntx" not in existing]

    if not fonts_to_export:
        return

    # base hiero command
    gcr = ut.gradle_cache_root()

    ver = ut.libgdx_version()
    gdx_jars = ["gdx-"+ver+".jar",
                "gdx-platform-"+ver+"-natives-desktop.jar",
                "gdx-backend-lwjgl-"+ver+".jar"]

    badlogic = j(gcr, "com.badlogicgames.gdx")
    gdx_jars = [ut.find_file(jar, badlogic) for jar in gdx_jars]

    if None in gdx_jars:
        raise Exception("Font gen: Can't find required gdx jars \
                (try running the game without any text objects first)")

    op_sys = {"lin":"linux", "dar":"osx", "win":"windows"}[sys.platform[:3]]

    lwjgl_jars = ["lwjgl-[0-9].[0-9].[0-9].jar",
                  "lwjgl-platform-*-natives-"+op_sys+".jar"]

    lwjgl = j(gcr, "org.lwjgl.lwjgl")
    lwjgl_jars = [ut.find_file(jar, lwjgl) for jar in lwjgl_jars]

    jars = gdx_jars + lwjgl_jars + [j(hiero_dir, "gdx-tools.jar")]

    sep = ";" if op_sys == "windows" else ":"
    hiero = 'java -cp "{}" com.badlogic.gdx.tools.hiero.Hiero '.format(sep.join(jars))

    # export fonts, via hiero
    for font in fonts_to_export:
        ttf = font.filepath
        if ttf == "<builtin>":
            ttf = j(hiero_dir, "bfont.ttf")
        else:
            ttf = os.path.abspath(bpy.path.abspath(ttf))
        hiero += '"{}---{}" '.format(ttf, j(fonts_dir, font.name))
    os.system(hiero)

    # move pngs to textures dir
    for f in ut.listdir_fullpath(fonts_dir, ".png"):
        shutil.move(f, j(textures_dir, "__FNT_" + os.path.basename(f)))

    # convert hiero-generated angel code files (.fnt), to proper json files (.fntx)
    fnts = ut.listdir_fullpath(fonts_dir, ".fnt")
    for fnt in fnts:
        with open(fnt+'x', 'w') as f:
            json.dump(ut.angel_code(fnt), f)

    # remove fnt files
    for f in fnts:
        os.remove(f)


scene = None;

def export(context, filepath, scene_name, exprun):
    global scene;
    scene = bpy.data.scenes[scene_name] if scene_name else context.scene

    objects = scene.objects

    ts = texts(objects)
    fonts = used_fonts(ts)

    bdx = {
        "name": scene.name,
        "models": srl_models(used_meshes(objects)),
        "objects": srl_objects(objects),
        "materials": srl_materials(used_materials(objects)),
        "cameras": camera_names(scene),
        "actions": srl_actions(bpy.data.actions),
        "fonts": [f.name for f in fonts]
    }

    if exprun:
        j = os.path.join

        bdx_dir = j(ut.project_root(), "android", "assets", "bdx")
        fonts_dir = j(bdx_dir, "fonts")
        textures_dir = j(bdx_dir, "textures")
        hiero_dir = j(ut.gen_root(), "hiero")

        generate_bitmap_fonts(fonts, hiero_dir, fonts_dir, textures_dir);

        bdx["models"].update(srl_models_text(ts, fonts_dir))
        bdx["materials"].update(srl_materials_text(fonts))

        # Generate instantiators
        lines = instantiator(objects)

        if lines:
            lines[5] = lines[5].replace("NAME", scene.name)

            inst = j(ut.src_root(), "inst")

            if not os.path.isdir(inst):
                os.mkdir(inst)

            with open(j(inst, scene.name + ".java"), 'w') as f:
                f.writelines(lines)

    with open(filepath, "w") as f:
        json.dump(bdx, f)

    return {'FINISHED'}


class ExportBdx(Operator, ExportHelper):
    """Export to bdx scene format (.bdx)"""
    bl_idname = "export_scene.bdx"
    bl_label = "Export to .bdx"

    filename_ext = ".bdx"

    filter_glob = StringProperty(
            default="*.bdx",
            options={'HIDDEN'},
            )

    scene_name = StringProperty(
            default="",
            )

    exprun = BoolProperty(
            default=False,
            )

    def execute(self, context):
        return export(context, self.filepath, self.scene_name, self.exprun)


def menu_func_export(self, context):
    self.layout.operator(ExportBdx.bl_idname, text="bdx (.bdx)")


def register():
    bpy.utils.register_class(ExportBdx)
    bpy.types.INFO_MT_file_export.append(menu_func_export)


def unregister():
    bpy.utils.unregister_class(ExportBdx)
    bpy.types.INFO_MT_file_export.remove(menu_func_export)


if __name__ == "__main__":
    register()
