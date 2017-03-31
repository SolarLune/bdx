import bpy
from .. import ui


class AddBdxComponent(bpy.types.Operator):
    """Add Component"""
    bl_idname = "object.add_bdx_component"
    bl_label = "Add Component Slot"

    def execute(self, context):

        context.object.bdx.components.add()

        return {"FINISHED"}


class RemoveBdxComponent(bpy.types.Operator):
    """Remove Component"""
    bl_idname = "object.remove_bdx_component"
    bl_label = "Remove Component Slot"

    index = bpy.props.IntProperty(name="index")

    def execute(self, context):

        context.object.bdx.components.remove(self.index)

        return {"FINISHED"}


class MoveBdxComponent(bpy.types.Operator):
    """Rearrange Component in List"""
    bl_idname = "object.move_bdx_component"
    bl_label = "Move Component"

    index = bpy.props.IntProperty(name="index")
    direction = bpy.props.StringProperty(name="UP")

    def execute(self, context):

        comps = context.object.bdx.components

        if self.direction == "UP" and self.index > 0:

            comps.move(self.index, self.index - 1)

        elif self.direction == "DOWN" and self.index < len(comps) - 1:

            comps.move(self.index, self.index + 1)

        return {"FINISHED"}


class AddComponentProperty(bpy.types.Operator):
    """Adds a property from a component"""
    bl_idname = "object.add_bdx_component_property"
    bl_label = "Add Component Property"

    comp_index = bpy.props.IntProperty(name="comp_index")

    def execute(self, context):
        comps = context.object.bdx.components
        comps[self.comp_index].props.add()

        return {"FINISHED"}


class RemoveComponentProperty(bpy.types.Operator):
    """Removes a property from a component"""
    bl_idname = "object.remove_bdx_component_property"
    bl_label = "Remove Component Property"

    comp_index = bpy.props.IntProperty(name="comp_index")
    prop_index = bpy.props.IntProperty(name="prop_index")

    def execute(self, context):

        comps = context.object.bdx.components
        comps[self.comp_index].props.remove(self.prop_index)

        return {"FINISHED"}


class MoveComponentProperty(bpy.types.Operator):
    """Moves a component's property"""
    bl_idname = "object.move_bdx_component_property"
    bl_label = "Move Component Property"

    comp_index = bpy.props.IntProperty(name="comp_index")
    prop_index = bpy.props.IntProperty(name="prop_index")
    direction = bpy.props.StringProperty(name="UP")

    def execute(self, context):

        comps = context.object.bdx.components

        if self.direction == "UP" and self.prop_index > 0:

            comps[self.comp_index].props.move(self.prop_index, self.prop_index - 1)

        elif self.direction == "DOWN" and self.prop_index < len(comps[self.comp_index].props) - 1:

            comps[self.comp_index].props.move(self.prop_index, self.prop_index + 1)

        return {"FINISHED"}


def register():
    bpy.utils.register_class(AddBdxComponent)
    bpy.utils.register_class(RemoveBdxComponent)
    bpy.utils.register_class(MoveBdxComponent)
    bpy.utils.register_class(AddComponentProperty)
    bpy.utils.register_class(RemoveComponentProperty)
    bpy.utils.register_class(MoveComponentProperty)


def unregister():
    bpy.utils.unregister_class(AddBdxComponent)
    bpy.utils.unregister_class(RemoveBdxComponent)
    bpy.utils.unregister_class(MoveBdxComponent)
    bpy.utils.unregister_class(AddComponentProperty)
    bpy.utils.unregister_class(RemoveComponentProperty)
    bpy.utils.unregister_class(MoveComponentProperty)
