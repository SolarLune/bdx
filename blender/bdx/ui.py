import os
import bpy
from . import utils as ut

S = bpy.types.Scene
P = bpy.props

S.bdx_proj_name = P.StringProperty(name="Project Name")
S.bdx_java_pack = P.StringProperty(name="Java Package")
S.bdx_base_path = P.StringProperty(name="Base Path", subtype='DIR_PATH')
S.bdx_dir_name = P.StringProperty(name="Directory")
S.bdx_android_sdk = P.StringProperty(name="Android SDK", subtype='DIR_PATH')

class BdxProject(bpy.types.Panel):
    """Crates the BDX panel in the render properties window"""
    bl_idname = "RENDER_PT_bdx"
    bl_label = "BDX"
    bl_space_type = 'PROPERTIES'
    bl_region_type = 'WINDOW'
    bl_context = "render"

    def draw(self, context):
        layout = self.layout

        r = layout.row

        if ut.in_bdx_project():
            r().label(text="In BDX project: " + ut.project_name())

            r().operator("object.bdxexprun", text="Export and Run")

        else:
            sc = context.scene

            if ut.in_packed_bdx_blend():
                r().label(text="In packed BDX blend.")
            else:
                r().prop(sc, "bdx_proj_name")
                r().prop(sc, "bdx_java_pack")
                r().prop(sc, "bdx_base_path")
                r().prop(sc, "bdx_dir_name")
                r().prop(sc, "bdx_android_sdk")

            r().operator("scene.create_bdx_project", text="Create BDX project")


def register():
    bpy.utils.register_class(BdxProject)

    @bpy.app.handlers.persistent
    def P_mapto_bdxexprun(dummy):
        """Override P to export and run BDX game, instead of running BGE game"""

        kmi = bpy.data.window_managers['WinMan'].keyconfigs['Blender'].keymaps['Object Mode'].keymap_items

        if ut.in_bdx_project():
            if 'view3d.game_start' in kmi:
                kmi['view3d.game_start'].idname = 'object.bdxexprun'
        else:
            if 'objects.bdxexprun' in kmi:
                kmi['objects.bdxexprun'].idname = 'view3d.game_start'

    bpy.app.handlers.load_post.append(P_mapto_bdxexprun)


def unregister():
    bpy.utils.unregister_class(BdxProject)


if __name__ == "__main__":
    register()
