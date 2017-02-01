import os
import bpy
from . import utils as ut

P = bpy.props

class BdxSceneProps(bpy.types.PropertyGroup):
    proj_name = P.StringProperty(name="Project Name")
    java_pack = P.StringProperty(name="Java Package")
    base_path = P.StringProperty(name="Base Path", subtype="DIR_PATH")
    dir_name = P.StringProperty(name="Directory")
    android_sdk = P.StringProperty(name="Android SDK", subtype="DIR_PATH")
    always_export_fonts = P.BoolProperty(name="Always Export Fonts", description="Whether BDX should always export fonts to texture, or only when they don't exist already", default=False)
    auto_export = P.BoolProperty(name="Auto-Export On Save", description="If BDX should automatically export game data when you save", default=False)
    main_scene = P.StringProperty(name="Starting Scene", description="Starting game scene; if blank, the current scene is used")

class BdxObjectProps(bpy.types.PropertyGroup):
    cls_use_custom = P.BoolProperty(name="", description="Use custom Java class for this object")
    cls_custom_name = P.StringProperty(name="", description="Java class name for this object")
    cls_use_priority = P.BoolProperty(name="", description="Use execution priority for this object")

class BdxFontProps(bpy.types.PropertyGroup):

    font_size = P.IntProperty(name="Font Size", description="Sets the font's size, in pixels, on its output texture", default=32, min=1)
    font_color = P.FloatVectorProperty(name="Font Color", description="Color of the font", subtype="COLOR", default=[1, 1, 1], min=0, max=1)
    font_alpha = P.FloatProperty(name="Font Alpha", description="Alpha of the font", default=1, min=0, max=1)

    font_shadow_offset = P.IntVectorProperty(name="Shadow Offset", description="Offset of the shadow (in pixels, relative to font size)", size=2, default=[0, 0], min=0)
    font_shadow_color = P.FloatVectorProperty(name="Shadow Color", description="Color of the shadow", subtype="COLOR", default=[0, 0, 0], min=0, max=1)
    font_shadow_alpha = P.FloatProperty(name="Shadow Alpha", description="Transparency of the shadow", default=1, min=0, max=1, step=0.1)

    font_outline_thickness = P.IntProperty(name="Outline Thickness", description="Thickness of the outline in pixels", default=0, min=0)
    font_outline_color = P.FloatVectorProperty(name="Outline Color", description="Color of outlines", subtype="COLOR", default=[0, 0, 0], min=0, max=1)
    font_outline_alpha = P.FloatProperty(name="Outline Alpha", description="Transparency of outlines", default=1, min=0, max=1, step=0.1)
    font_outline_rounded = P.BoolProperty(name="Rounded Outlines", description="Whether to have rounded outlines or not", default=False)

bpy.utils.register_class(BdxSceneProps)
bpy.utils.register_class(BdxObjectProps)
bpy.utils.register_class(BdxFontProps)

bpy.types.Scene.bdx = P.PointerProperty(type=BdxSceneProps)
bpy.types.Object.bdx = P.PointerProperty(type=BdxObjectProps)
bpy.types.VectorFont.bdx = P.PointerProperty(type=BdxFontProps)

prop_move_support = version = float("{}.{}".format(*bpy.app.version)) >= 2.75


class BdxProject(bpy.types.Panel):
    """Creates the BDX panel in the render properties window"""
    bl_idname = "RENDER_PT_bdx"
    bl_label =  "BDX"
    bl_space_type = "PROPERTIES"
    bl_region_type = "WINDOW"
    bl_context = "render"

    def draw(self, context):
        layout = self.layout

        r = layout.row

        sc_bdx = context.scene.bdx

        if ut.in_bdx_project():
            r().label(text="In BDX project: " + ut.project_name())

            b = layout.box()
            b.operator("object.bdxexprun")
            b.operator("object.bdxexp")
            b.operator("object.bdxrun")

            r().prop(sc_bdx, "main_scene")
            r().prop(sc_bdx, "auto_export")
            r().operator("object.packproj")
            r().operator("object.externjava")

        else:

            if ut.in_packed_bdx_blend():
                r().label(text="In packed BDX blend.")
            else:
                r().prop(sc_bdx, "proj_name")
                r().prop(sc_bdx, "java_pack")
                r().prop(sc_bdx, "base_path")
                r().prop(sc_bdx, "dir_name")
                r().prop(sc_bdx, "android_sdk")

            r().operator("scene.create_bdx_project", text="Create BDX project")


class BdxObject(bpy.types.Panel):
    """Creates the BDX Panel in the Object properties window"""
    bl_label = "BDX"
    bl_idname = "OBJECT_PT_bdx"
    bl_space_type = "PROPERTIES"
    bl_region_type = "WINDOW"
    bl_context = "object"

    def draw(self, context):
        ob = context.object
        ob_bdx = ob.bdx
        game = ob.game

        layout = self.layout

        row = layout.row()
        col = row.column()
        if ob_bdx.cls_use_priority:
            col.prop(ob_bdx, "cls_use_priority", icon="FONTPREVIEW")
        else:
            col.prop(ob_bdx, "cls_use_priority", icon="BOOKMARKS")
        col = row.column()
        if ob_bdx.cls_use_custom:
            col.active = True
            col.prop(ob_bdx, "cls_custom_name")
        else:
            col.active = False
            col.label(ob.name + ".java")
        col = row.column()
        col.prop(ob_bdx, "cls_use_custom")
        
        row = layout.row(align=True)
        is_font = (ob.type == "FONT")
        if is_font:
            prop_index = game.properties.find("Text")
            if prop_index != -1:
                layout.operator("object.game_property_remove", text="Remove Text Game Property", icon="X").index = prop_index
                row = layout.row()
                sub = row.row()
                sub.enabled = 0
                prop = game.properties[prop_index]
                sub.prop(prop, "name", text="")
                row.prop(prop, "type", text="")
                row.label("See Text Object")
            else:
                props = layout.operator("object.game_property_new", text="Add Text Game Property", icon="ZOOMIN")
                props.name = "Text"
                props.type = "STRING"

        props = layout.operator("object.game_property_new", text="Add Game Property", icon="ZOOMIN")
        props.name = ""

        for i, prop in enumerate(game.properties):
            if is_font and i == prop_index:
                continue
            box = layout.box()
            row = box.row()
            row.prop(prop, "name", text="")
            row.prop(prop, "type", text="")
            row.prop(prop, "value", text="")
            row.prop(prop, "show_debug", text="", toggle=True, icon="INFO")
            if prop_move_support:
                sub = row.row(align=True)
                props = sub.operator("object.game_property_move", text="", icon="TRIA_UP")
                props.index = i
                props.direction = "UP"
                props = sub.operator("object.game_property_move", text="", icon="TRIA_DOWN")
                props.index = i
                props.direction = "DOWN"
            row.operator("object.game_property_remove", text="", icon="X", emboss=False).index = i


class BdxData(bpy.types.Panel):
    """Creates the BDX Panel in the Object's Data properties window"""
    bl_label = "BDX"
    bl_idname = "DATA_PT_bdx"
    bl_space_type = "PROPERTIES"
    bl_region_type = "WINDOW"
    bl_context = "data"

    def draw(self, context):

        if type(context.object.data) == bpy.types.TextCurve:

            txt_bdx = context.object.data.font.bdx

            layout = self.layout

            layout.row().prop(txt_bdx, "font_size")

            row = layout.row()
            row.prop(txt_bdx, "font_color")
            row.prop(txt_bdx, "font_alpha")

            layout.row().prop(txt_bdx, "font_shadow_offset")
            if txt_bdx.font_shadow_offset[0] != 0 or txt_bdx.font_shadow_offset[1] != 0:
                box = layout.box()
                box.prop(txt_bdx, "font_shadow_color")
                box.prop(txt_bdx, "font_shadow_alpha")

            row = layout.row()

            row.prop(txt_bdx, "font_outline_thickness")
            if txt_bdx.font_outline_thickness > 0:
                box = layout.box()

                box.prop(txt_bdx, "font_outline_color")
                box.prop(txt_bdx, "font_outline_alpha")
                box.prop(txt_bdx, "font_outline_rounded")

            layout.box().prop(context.scene.bdx, "always_export_fonts")


def register():
    bpy.utils.register_class(BdxProject)
    bpy.utils.register_class(BdxObject)
    bpy.utils.register_class(BdxData)

    @bpy.app.handlers.persistent
    def export_on_save(dummy):
        if bpy.context.scene.bdx.auto_export:
            bpy.ops.object.bdxexp()

    @bpy.app.handlers.persistent
    def P_mapto_bdxexprun(dummy):
        """Override P to export and run BDX game, instead of running BGE game"""

        keymaps = bpy.data.window_managers["WinMan"].keyconfigs["Blender"].keymaps
        kmi = keymaps["Object Mode"].keymap_items if "Object Mode" in keymaps else None

        if kmi:

            if ut.in_bdx_project():
                if "view3d.game_start" in kmi:
                    kmi["view3d.game_start"].idname = "object.bdxexprun"
            else:
                if "objects.bdxrun" in kmi:
                    kmi["objects.bdxexprun"].idname = "view3d.game_start"

    bpy.app.handlers.load_post.append(P_mapto_bdxexprun)
    bpy.app.handlers.save_post.append(export_on_save)


def unregister():
    bpy.utils.unregister_class(BdxProject)
    bpy.utils.unregister_class(BdxObject)
    bpy.utils.unregister_class(BdxData)


if __name__ == "__main__":
    register()
