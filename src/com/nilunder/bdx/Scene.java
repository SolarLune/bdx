package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.Transform;
import com.nilunder.bdx.gl.RenderBuffer;
import com.nilunder.bdx.gl.ShaderProgram;
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.components.*;
import com.nilunder.bdx.GameObject.ArrayListGameObject;

public class Scene implements Named{
	public static HashMap<String, Instantiator> instantiators;

	public JsonValue json;

	public String name;
	public LinkedListNamed<GameObject> objects;
	public LinkedListNamed<Light> lights;
	public Camera camera;
	public ArrayList<Camera> cameras;
	public HashMap<Model, Vector2f> modelToFrame;

	private FileHandle scene;

	public HashMap<String,Model> models;
	private HashMap<String,Texture> textures;
	public HashMap<String,Material> materials;
	public Material defaultMaterial;
	private Model defaultModel;
	public DiscreteDynamicsWorld world;

	private ArrayList<GameObject> toBeAdded;
	private ArrayList<GameObject> toBeRemoved;

	private boolean requestedRestart;
	public boolean paused;
	
	private Instantiator instantiator;
	
	public HashMap<String, GameObject> templates;
	public ArrayList<ShaderProgram> filters;
	public RenderBuffer lastFrameBuffer;
	public Environment environment;
	static private ShapeRenderer shapeRenderer;

	public Scene(String name){
		this(Gdx.files.internal("bdx/scenes/" + name + ".bdx"), instantiators.get(name));
	}

	public Scene(FileHandle scene, Instantiator instantiator){
		this.scene = scene;
		if (instantiator != null){
			this.instantiator = instantiator;
		}else{
			this.instantiator = new Instantiator();
		}
	}

	public Vector3f gravity(){
		return world.getGravity(new Vector3f());
	}

	public void gravity(Vector3f gravity){
		world.setGravity(gravity);
	}

	public String name(){
		return name;
	}

	public static class BDXIntAttribute extends IntAttribute {

		public final static String ShadelessAlias = "Shadeless";
		public final static long Shadeless = register(ShadelessAlias);

		public BDXIntAttribute(){
			super(Shadeless, 0);
		}

	};

	public static class BDXColorAttribute extends ColorAttribute {

		public final static String TintAlias = "Tint";
		public final static long Tint = register(TintAlias);
		public final static String EmitAlias = "Emit";
		public final static long Emit = register(EmitAlias);

		static {
			Mask = Mask | Tint | Emit;
		}

		private BDXColorAttribute(long type, float r, float g, float b){
			super(type, r, g, b, 0);
		}

	}

	public void init(){
		requestedRestart = false;
		paused = false;

		if (shapeRenderer == null)
			shapeRenderer = new ShapeRenderer();
		lastFrameBuffer = new RenderBuffer(null);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0, 0, 0, 1));
		environment.set(new PointLightsAttribute());
		environment.set(new SpotLightsAttribute());
		environment.set(new DirectionalLightsAttribute());
				
		filters = new ArrayList<ShaderProgram>();
		defaultMaterial = new Material();
		defaultMaterial.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1));
		defaultMaterial.set(new ColorAttribute(ColorAttribute.Diffuse, 1, 1, 1, 1));
		defaultMaterial.set(new BlendingAttribute());
		defaultMaterial.set(new BDXColorAttribute(BDXColorAttribute.Tint, 0, 0, 0));
		defaultModel = new ModelBuilder().createBox(1.0f, 1.0f, 1.0f, defaultMaterial, Usage.Position | Usage.Normal | Usage.TextureCoordinates);

		models = new HashMap<String,Model>();
		textures = new HashMap<String,Texture>();
		materials = new HashMap<String,Material>();
		modelToFrame = new HashMap<>();

		materials.put("__BDX_DEFAULT", defaultMaterial);
		
		BroadphaseInterface broadphase = new DbvtBroadphase();
		DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
		SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();
		CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);

		toBeAdded = new ArrayList<GameObject>();
		toBeRemoved = new ArrayList<GameObject>();
		objects = new LinkedListNamed<GameObject>();
		lights = new LinkedListNamed<Light>();
		templates = new HashMap<String, GameObject>();
		
		json = new JsonReader().parse(scene);
		name = json.get("name").asString();
		
		world = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
		world.setDebugDrawer(new Bullet.DebugDrawer(json.get("physviz").asBoolean()));
		gravity(new Vector3f(0, 0, -json.get("gravity").asFloat()));
		ambientLight(new Vector3f(json.get("ambientColor").asFloatArray()));

		Bdx.profiler.init(json.get("framerateProfile").asBoolean());

		for (JsonValue mat : json.get("materials")){
			String texName = mat.get("texture").asString();

			float[] c = mat.get("color").asFloatArray();
			Material material = new Material(ColorAttribute.createDiffuse(c[0], c[1], c[2], 1));

			float[] s = mat.get("spec_color").asFloatArray();

			material.set(ColorAttribute.createSpecular(s[0], s[1], s[2], 1));

			material.set(FloatAttribute.createShininess(mat.get("shininess").asFloat()));

			material.set(new BDXColorAttribute(BDXColorAttribute.Tint, 0, 0, 0));

			IntAttribute shadeless = (IntAttribute) new BDXIntAttribute();

			if (mat.get("shadeless").asBoolean())
				shadeless.value = 1;

			material.set(shadeless);

			material.id = mat.name;

			float emitStrength = mat.get("emit").asFloat();
			material.set(new BDXColorAttribute(BDXColorAttribute.Emit, emitStrength, emitStrength, emitStrength));

			if (mat.get("backface_culling").asBoolean())
				material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_BACK));
			else
				material.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
			
			if (texName != null){
				Texture texture = textures.get(texName);
				if (texture == null){
					texture = new Texture(Gdx.files.internal("bdx/textures/" + texName));
					textures.put(texName, texture);
				}
				material.set(TextureAttribute.createDiffuse(texture));
				texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
			}
			
			BlendingAttribute ba;

			if (mat.get("alpha_blend").asString().equals("ALPHA")) {
				ba = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
				ba.opacity = mat.get("opacity").asFloat();
				material.set(ba);
				material.set(FloatAttribute.createAlphaTest(0));
			}
			else {
				ba = new BlendingAttribute();
				ba.blended = false;
				material.set(ba);
			}

			materials.put(mat.name, material);
		}
		
		for (JsonValue model: json.get("models")){
			models.put(model.name, createModel(model));
		}

		HashMap<String, JsonValue> fonts = new HashMap<>();
		for (JsonValue fontj: json.get("fonts")){
			String font = fontj.asString();
			fonts.put(font, new JsonReader().parse(Gdx.files.internal("bdx/fonts/"+font+".fntx")));
		}

		FAnim.loadActions(json.get("actions"));

		for (JsonValue gobj: json.get("objects")){
			GameObject g = instantiator.newObject(gobj);

			String type = gobj.get("type").asString();
			if (type.equals("FONT")){
				Text t = (Text)g;
				t.font = fonts.get(gobj.get("font").asString());
			}
			else if (type.equals("LAMP")) {
				JsonValue settings = gobj.get("lamp");
				Light l = (Light)g;

				if (settings.getString("type").equals("POINT"))
					l.type = Light.Type.POINT;
				else if (settings.getString("type").equals("SUN"))
					l.type = Light.Type.SUN;
				else if (settings.getString("type").equals("SPOT"))
					l.type = Light.Type.SPOT;

				l.energy(settings.getFloat("energy"));
				float[] c = settings.get("color").asFloatArray();
				l.color(new Color(c[0], c[1], c[2], c[3]));

				if (l.type.equals(Light.Type.SPOT)) {
					l.spotSize(settings.getFloat("spot_size"));
				}
			}

			g.name = gobj.name;

			String modelName = gobj.get("mesh_name").asString();
			if (modelName != null){
				g.visibleNoChildren(gobj.get("visible").asBoolean());
				g.modelInstance = new ModelInstance(models.get(modelName));
			}else{
				g.visibleNoChildren(false);
				g.modelInstance = new ModelInstance(defaultModel);
			}
			Mesh mesh = g.modelInstance.model.meshes.first();
			float[] trans = gobj.get("transform").asFloatArray();
			JsonValue origin = json.get("origins").get(modelName);
			JsonValue dimensions = json.get("dimensions").get(modelName);
			g.origin = origin == null ? new Vector3f() : new Vector3f(origin.asFloatArray());
			g.dimensionsNoScale = dimensions == null ? new Vector3f(1, 1, 1) : new Vector3f(dimensions.asFloatArray());
			JsonValue physics = gobj.get("physics");
			g.currBodyType = physics.get("body_type").asString();
			g.currBoundsType = physics.get("bounds_type").asString();
			g.body = Bullet.makeBody(mesh, trans, g.origin, g.currBodyType, g.currBoundsType, physics);
			g.body.setUserPointer(g);
			
			g.props = new HashMap<String, JsonValue>();
			for (JsonValue prop : gobj.get("properties")){
				g.props.put(prop.name, prop);
			}
			
			g.json = gobj;

			g.scene = this;

			g.scale(getGLMatrixScale(trans));

			templates.put(g.name, g);
			
		}

		hookParentChild();
		
		addInstances();
		
		cameras = new ArrayList<Camera>();
		String[] cameraNames = json.get("cameras").asStringArray();
		for (String cn : cameraNames)
			cameras.add((Camera) objects.get(cn));
		
		camera = cameras.get(0);

		ArrayList<GameObject> rootParents = new ArrayList<GameObject>();

		for (GameObject g : objects){
			if (g.parent() == null){
				rootParents.add(g);
			}
		}

		for (GameObject g : rootParents){
			initGameObject(g);
		}

	}

	public void dispose(){
		lastFrameBuffer.dispose();
		defaultModel.dispose();

		for (Model m : models.values()){
			m.dispose();
		}
		for (Texture t : textures.values()){
			t.dispose();
		}
		for (ShaderProgram s : filters) {
			s.dispose();
		}
	}
	
	private void hookParentChild(){
		for (GameObject g : templates.values()){
			String parentName = g.json.get("parent").asString();
			if (parentName != null){
				g.parent(templates.get(parentName));
			}
		}
	}

	private void addInstances(){

		ArrayList<GameObject> temps = new ArrayList<GameObject>(templates.values());

		for (GameObject t : temps){
			if (t.json.get("use_priority").asBoolean()){
				Collections.swap(temps, 0, temps.indexOf(t));
				break;
			}
		}

		for (GameObject gobj : temps){
			boolean onActiveLayer = gobj.json.get("active").asBoolean();
			if (onActiveLayer && gobj.parent() == null){
				GameObject g = clone(gobj);
				addToWorld(g);
			}
		}

		for (GameObject g : toBeAdded) {
			objects.add(g);
			if (g instanceof Light)
				lights.add((Light) g);
		}

		toBeAdded.clear();
	}

	private GameObject cloneNoChildren(GameObject gobj){
		GameObject g = instantiator.newObject(gobj.json);

		g.json = gobj.json;
		
		g.name = gobj.name;
		g.visibleNoChildren(gobj.visible());
		g.modelInstance = new ModelInstance(gobj.modelInstance);
		
		g.body = Bullet.cloneBody(gobj.body);
		g.currBodyType = gobj.currBodyType;
		g.currBoundsType = gobj.currBoundsType;
		g.origin = gobj.origin;
		g.dimensionsNoScale = gobj.dimensionsNoScale;
		g.body.setUserPointer(g);
		g.scale(gobj.scale());
		
		g.props = gobj.props;
		
		g.scene = this;

		if (g instanceof Camera){
			Camera c = (Camera)g;
			c.data.projection.set(c.json.get("camera").get("projection").asFloatArray());
		}else if (g instanceof Text){
			Text t = (Text)g;
			Text tt = (Text)gobj;
			t.font = tt.font;
			t.useUniqueModel();
		}
		else if (g instanceof Light) {
			Light l = (Light)g;
			Light ll = (Light)gobj;
			l.energy(ll.energy());
			l.color(ll.color());
			l.spotSize(ll.spotSize());
			l.exponent(ll.exponent());
			l.type = ll.type;
			l.makeLightData();
			l.updateLight();

			if (l.lightData instanceof PointLight) {
				PointLightsAttribute la = (PointLightsAttribute) environment.get(PointLightsAttribute.Type);
				la.lights.add((PointLight) l.lightData);
			}
			else if (l.lightData instanceof DirectionalLight) {
				DirectionalLightsAttribute la = (DirectionalLightsAttribute) environment.get(DirectionalLightsAttribute.Type);
				la.lights.add((DirectionalLight) l.lightData);
			}
			else if (l.lightData instanceof SpotLight) {
				SpotLightsAttribute la = (SpotLightsAttribute) environment.get(SpotLightsAttribute.Type);
				la.lights.add((SpotLight) l.lightData);
			}

		}

		return g;
	}

	private GameObject clone(GameObject gobj){
		String instance = gobj.json.get("instance").asString();

		GameObject inst = gobj;
		if (instance != null)
			gobj = templates.get(instance);

		GameObject g = cloneNoChildren(gobj);

		for (GameObject c : gobj.children){
			GameObject nc = clone(c);
			nc.parent(g);
		}

		if (instance != null){
			g.position(inst.position());
			Matrix3f ori = inst.orientation();
			ori.mul(g.orientation());
			g.orientation(ori);
			g.scale(inst.scale());

			g.props = new HashMap<String, JsonValue>(g.props);
			g.props.putAll(inst.props);

			for (GameObject c : inst.children){
				GameObject nc = clone(c);
				nc.parent(g);
			}
		}

		return g;

	}
	
	private void initGameObject(GameObject gobj){
		gobj.init();

		ArrayList<GameObject> children = new ArrayList<GameObject>(gobj.children);

		for (GameObject c : children){
			initGameObject(c);
		}
	}

	private void addToWorld(GameObject gobj){
		if (!gobj.currBodyType.equals("NO_COLLISION")){
			world.addRigidBody(gobj.body, gobj.json.get("physics").get("group").asShort(), gobj.json.get("physics").get("mask").asShort());
			if (gobj.currBodyType.equals("STATIC") || gobj.currBodyType.equals("SENSOR"))
				gobj.deactivate();
			if (gobj.parent() != null && gobj.parent().body.getCollisionShape().isCompound())
				world.removeRigidBody(gobj.body);
		}
		
		toBeAdded.add(gobj);
		
		for (GameObject g : gobj.children){
			addToWorld(g);
		}
	}
	
	public GameObject add(GameObject gobj){		
		GameObject g = clone(gobj);
		addToWorld(g);
		initGameObject(g);
		
		return g;
	}
	
	public GameObject addNoChildren(GameObject gobj){
		GameObject g = cloneNoChildren(gobj);
		addToWorld(g);
		initGameObject(g);

		return g;
	}
	
	public GameObject add(String name){
		return add(templates.get(name));
	}
	
	public GameObject addNoChildren(String name){
		return addNoChildren(templates.get(name));
	}
	
	public void remove(GameObject g){
		toBeAdded.remove(g);
		world.removeRigidBody(g.body);
		toBeRemoved.add(g);
	}

	public RayHit ray(Vector3f src, Vector3f vec){
		return ray(src, vec, (short)~0, (short)~0);
	}
	
	public RayHit ray(Vector3f src, Vector3f vec, short group, short mask){
		Vector3f to = new Vector3f(src);
		to.add(vec);
		
		CollisionWorld.ClosestRayResultCallback rrc = new CollisionWorld.ClosestRayResultCallback(src, to);
		rrc.collisionFilterGroup = group;
		rrc.collisionFilterMask = mask;
		
		world.rayTest(src, to, rrc);
		
		if (!rrc.hasHit())
			return null;
		
		RayHit rh = new RayHit();
		rh.object = (GameObject) (rrc.collisionObject.getUserPointer());
		rh.position = rrc.hitPointWorld;
		rh.normal = rrc.hitNormalWorld;
		
		return rh;
	}

	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec, boolean includeAll){
		return xray(src, vec, includeAll, (short)~0, (short)~0);
	}
	
	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec, boolean includeAll, short group, short mask){

		Vector3f startPos = new Vector3f(src);
		Vector3f dist = new Vector3f(vec);

		ArrayList<RayHit> hits = new ArrayList<RayHit>();
		ArrayList<GameObject> hitObjects = new ArrayList<GameObject>();

		boolean finished = false;

		while (!finished){

			RayHit ray = ray(startPos, dist, group, mask);

			if (ray != null){

				if (!hitObjects.contains(ray.object) || includeAll) {

					hits.add(ray);
					hitObjects.add(ray.object);

				}

				Vector3f delta = ray.position.minus(startPos);

				if (delta.length() == 0) {
					delta = new Vector3f(dist);
					delta.length(0.001f);
				}

				startPos.add(delta);
				dist.sub(delta);

			}
			else
				finished = true;

		}

		return hits;

	}

	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec) {
		return xray(src, vec, false);
	}

	public Model createModel(JsonValue model) {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		short idx = 0;
		for (JsonValue mat : model){
			MeshPartBuilder mpb = builder.part(model.name, GL20.GL_TRIANGLES,
					Usage.Position | Usage.Normal | Usage.TextureCoordinates, materials.get(mat.name));
			float verts[] = mat.asFloatArray();
			mpb.vertex(verts);
			int len = verts.length / Bdx.VERT_STRIDE;
			for (short i = 0; i < len; ++i){
				mpb.index(idx);
				idx += 1;
			}
		}
		return builder.end();
	}
	
	public void restart(){
		requestedRestart = true;
	}
	
	public void pause(){
		paused = true;
	}
	
	public void play(){
		paused = false;
	}
	
	private void detectCollisions(){
		for (GameObject g : objects){
			ArrayListGameObject hitLast = g.touchingObjectsLast;
			g.touchingObjectsLast = g.touchingObjects;
			g.touchingObjects = hitLast;
			g.touchingObjects.clear();
			g.contactManifolds.clear();
		}
		
		int numManifolds = world.getDispatcher().getNumManifolds();
		
		for (int i = 0; i < numManifolds; ++i){
			PersistentManifold mani = world.getDispatcher().getManifoldByIndexInternal(i);
			if (mani.getNumContacts() > 0){
				RigidBody a = (RigidBody)mani.getBody0();
				RigidBody b = (RigidBody)mani.getBody1();
				GameObject A = (GameObject)a.getUserPointer();
				GameObject B = (GameObject)b.getUserPointer();
				A.touchingObjects.add(B);
				B.touchingObjects.add(A);
				A.contactManifolds.add(mani);
				B.contactManifolds.add(mani);
			}
		}
	}

	private void setGLMatrixScale(float[] m, Vector3f scale){
		m[0] *= scale.x;
		m[1] *= scale.x;
		m[2] *= scale.x;
		m[4] *= scale.y;
		m[5] *= scale.y;
		m[6] *= scale.y;
		m[8] *= scale.z;
		m[9] *= scale.z;
		m[10] *= scale.z;
	}

	private Vector3f getGLMatrixScale(float[] m){
		Vector3f s = new Vector3f();
		s.x = m[0];
		s.y = m[1];
		s.z = m[2];
		float x = s.length();
		s.x = m[4];
		s.y = m[5];
		s.z = m[6];
		float y = s.length();
		s.x = m[8];
		s.y = m[9];
		s.z = m[10];
		float z = s.length();
		s.x = x; s.y = y; s.z = z;
		return s;
	}
	
	private void updateVisuals(){
		Transform trans = new Transform();
		Vector3f scale = new Vector3f();
		float[] mt = new float[16];
		
		for (GameObject g : objects){
			if (g.visible()){
				g.body.getWorldTransform(trans);
				trans.getOpenGLMatrix(mt);
				g.body.getCollisionShape().getLocalScaling(scale);
				setGLMatrixScale(mt, scale);
				g.modelInstance.transform.set(mt);
			}
		}
	}
	
	private void runObjectLogic(){
		if (requestedRestart){
			for (GameObject g : objects){
				g.endNoChildren();
			}
			dispose();
			init();
		}

		Bdx.mouse.scene = this;

		for (Finger f : Bdx.fingers){
			f.scene = this;
		}

		for (GameObject g : objects){
			if(!g.valid())
				continue;
			if (g instanceof Light)
				((Light) g).updateLight();
			for (Component c : g.components){
				if (c.state != null)
					c.state.main();
			}
			g.main();
		}

		for (GameObject g : toBeAdded) {
			objects.add(g);
			if (g instanceof Light)
				lights.add((Light) g);
		}
		toBeAdded.clear();

		for (GameObject g : toBeRemoved) {
			objects.remove(g);
			if (g instanceof Light)
				lights.remove(g);
		}

		toBeRemoved.clear();

	}
	
	private void updateChildBodies(){
		for (GameObject g : objects){
			if (g.parent() == null && g.children.size() > 0 && g.body.isActive()){
				g.updateChildTransforms();
			}
		}
	}
	
	private void updateCamera(){
		// MVP
		Transform t = new Transform();
		float[] m = new float[16];
		camera.body.getWorldTransform(t);
		camera.data.position.set(t.origin.x, t.origin.y, t.origin.z);
		t.inverse();
		t.getOpenGLMatrix(m);
		camera.data.view.set(m);
		camera.data.combined.set(camera.data.projection);
		Matrix4.mul(camera.data.combined.val, camera.data.view.val);

		// Frustum 
		camera.data.invProjectionView.set(camera.data.combined);
		Matrix4.inv(camera.data.invProjectionView.val);
		camera.data.frustum.update(camera.data.invProjectionView);

	}
	
	public void ambientLight(Vector3f color){
		ambientLight(color.x, color.y, color.z);
	}
	
	public void ambientLight(float r, float g, float b) {
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		ca.color.set(r, g, b, 1);
	}
	
	public Vector3f ambientLight(){
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		return new Vector3f(ca.color.r, ca.color.g, ca.color.b);
	}
	
	public void update(){
		
		if (!paused){

			Bdx.profiler.start("__logic");
			runObjectLogic();			
			Bdx.profiler.stop("__logic");
			
			updateVisuals();
			Bdx.profiler.stop("__visuals");
			
			updateCamera();
			Bdx.profiler.stop("__camera");
			
			try{
				world.stepSimulation(Bdx.TICK_TIME, 0);
			}catch (NullPointerException e){
				throw new RuntimeException("PHYSICS ERROR: Detected collision between Static objects set to Ghost, with Triangle Mesh bounds: Keep them seperated, or use different bounds.");
			}
			Bdx.profiler.stop("__worldstep");
			
			updateChildBodies();
			Bdx.profiler.stop("__children");
			
			detectCollisions();
			Bdx.profiler.stop("__collisions");
			
		}

	}

	public void end(){

		for (GameObject g : objects)
			g.end();

		dispose();

		if (Bdx.scenes.contains(this)) {

			if (Bdx.scenes.size() > 1)
				Bdx.scenes.remove(this);
			else
				Bdx.end();

		}

	}

	public String toString(){

		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());

	}

	public void drawLine(Vector3f start, Vector3f end, Vector4f color){
		shapeRenderer.setProjectionMatrix(camera.data.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(color.x, color.y, color.z, color.w);
		shapeRenderer.line(start.x, start.y, start.z, end.x, end.y, end.z);
		shapeRenderer.end();
	}

	public void drawPoint(Vector3f point, Vector4f color){
		shapeRenderer.setProjectionMatrix(camera.data.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Point);
		shapeRenderer.setColor(color.x, color.y, color.z, color.w);
		shapeRenderer.point(point.x, point.y, point.z);
		shapeRenderer.end();
	}

}
