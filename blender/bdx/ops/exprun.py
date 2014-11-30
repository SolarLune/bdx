import os
import bpy
import subprocess
from .. import utils as ut


class BdxExpRun(bpy.types.Operator):
    """Export scenes to .bdx files, and run the BDX simulation"""
    bl_idname = "object.bdxexprun"
    bl_label = "BDX Export and Run"

    def execute(self, context):
        j = os.path.join

        proot = ut.project_root()
        sroot = ut.src_root()
        asset_dir = os.path.join(proot, "android", "assets", "bdx", "scenes")


        # Export scenes:

        for scene in bpy.data.scenes:
            file_name =  scene.name + ".bdx"
            file_path = j(asset_dir, file_name)

            bpy.ops.export_scene.bdx(filepath=file_path, scene_name=scene.name, exprun=True)


        # Modify relevant files:
        bdx_app = j(sroot, "BdxApp.java")

        # - BdxApp.java
        new_lines = []
        for scene in bpy.data.scenes:
            if os.path.isfile(j(sroot, "inst", scene.name + ".java")):
                inst = "new " + ut.package_name() + ".inst." + scene.name + "()"
            else:
                inst = "null"

            new_lines.append('("{}", {});'.format(scene.name, inst))


        put = "\t\tScene.instantiators.put"

        ut.remove_lines_containing(bdx_app, put)

        ut.insert_lines_after(bdx_app, "Scene.instantiators =", [put + l for l in new_lines])

        scene = bpy.context.scene
        ut.replace_line_containing(bdx_app, "scenes.add", '\t\tBdx.scenes.add(new Scene("'+scene.name+'"));');

        # - DesktopLauncher.java
        rx = str(scene.render.resolution_x)
        ry = str(scene.render.resolution_y)

        dl = j(ut.src_root("desktop", "DesktopLauncher.java"), "DesktopLauncher.java")
        ut.set_file_var(dl, "title", '"'+ut.project_name()+'"')
        ut.set_file_var(dl, "width", rx)
        ut.set_file_var(dl, "height", ry)

        # - AndroidLauncher.java
        al = j(ut.src_root("android", "AndroidLauncher.java"), "AndroidLauncher.java")
        ut.set_file_var(al, "width", rx)
        ut.set_file_var(al, "height", ry)

        # Run engine:
        context.window.cursor_set("WAIT")

        gradlew = "gradlew"
        if os.name != "posix":
            gradlew += ".bat"
        
        print(" ")
        print("------------ BDX START --------------------------------------------------")
        print(" ")
        error = subprocess.check_call([os.path.join(proot, gradlew), "-p", proot, "desktop:run"])
        print(" ")
        print("------------ BDX END ----------------------------------------------------")
        print(" ")

        if error:
            self.report({"ERROR"}, "BDX BUILD FAILED")
        
        context.window.cursor_set("DEFAULT")
        
        return {'FINISHED'}


def register():
    bpy.utils.register_class(BdxExpRun)


def unregister():
    bpy.utils.unregister_class(BdxExpRun)
