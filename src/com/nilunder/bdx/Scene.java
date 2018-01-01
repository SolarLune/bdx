package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
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

import com.nilunder.bdx.gl.*;
import com.nilunder.bdx.gl.Mesh;
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.components.*;
import com.nilunder.bdx.GameObject.BodyType;
import com.nilunder.bdx.GameObject.BoundsType;
import com.nilunder.bdx.utils.Color;

public class Scene implements Named{

	public static HashMap<String, Instantiator> instantiators;

	public JsonValue json;

	public String name;
	public LinkedListNamed<GameObject> objects;
	public LinkedListNamed<Light> lights;
	public Camera camera;
	public ArrayListNamed<Camera> cameras;
	public HashMap<Model, Vector2f> modelToFrame;
	public boolean visible;

	private FileHandle scene;

	public HashMap<String, Mesh> meshes;
	public ArrayList<Mesh> meshCopies;
	public HashMap<String,Texture> textures;
	public HashMap<String,Material> materials;
	public Material defaultMaterial;
	private Mesh defaultMesh;
	public DiscreteDynamicsWorld world;

	private ArrayList<GameObject> toBeAdded;
	private ArrayList<GameObject> toBeRemoved;

	private boolean requestedRestart;
	public boolean paused;
	
	private Instantiator instantiator;
	
	public Viewport viewport;
	public HashMap<String, GameObject> templates;
	public ArrayList<ScreenShader> screenShaders;
	public boolean renderPassthrough = true;
	public RenderBuffer lastFrameBuffer;
	public Environment environment;
	static private ShapeRenderer shapeRenderer;
	private ArrayList<ArrayList<Object>> drawCommands;
	static public boolean clearColorDefaultSet;

	private Color fogColor;
	private float fogStart;
	private float fogDepth;
	private boolean fogOn;
	private boolean valid;
	private boolean requestedEnd;

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
		requestedEnd = false;
		paused = false;
		visible = true;

		if (shapeRenderer == null)
			shapeRenderer = new ShapeRenderer();
		drawCommands = new ArrayList<ArrayList<Object>>();
		lastFrameBuffer = new RenderBuffer(null);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0, 0, 0, 1));
		environment.set(new PointLightsAttribute());
		environment.set(new SpotLightsAttribute());
		environment.set(new DirectionalLightsAttribute());
				
		screenShaders = new ArrayList<ScreenShader>();
		defaultMaterial = new Material("__BDX_DEFAULT");
		defaultMaterial.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1));
		defaultMaterial.set(new ColorAttribute(ColorAttribute.Diffuse, 1, 1, 1, 1));
		defaultMaterial.set(new BlendingAttribute());
		defaultMaterial.set(new BDXColorAttribute(BDXColorAttribute.Tint, 0, 0, 0));
		defaultMesh = new Mesh(new ModelBuilder().createBox(1.0f, 1.0f, 1.0f, defaultMaterial, Usage.Position | Usage.Normal | Usage.TextureCoordinates), this, "__BDX_DEFAULT");
		defaultMesh.autoDispose = false;

		meshes = new HashMap<String, Mesh>();
		meshCopies = new ArrayList<Mesh>();
		textures = new HashMap<String,Texture>();
		materials = new HashMap<String,Material>();
		modelToFrame = new HashMap<>();

		materials.put(defaultMaterial.id, defaultMaterial);
		
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

		if (Bdx.maxSubsteps() <= 0)
			Bdx.maxSubsteps(json.get("maxSubsteps").asInt());
		
		world = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
		world.setDebugDrawer(new Bullet.DebugDrawer(json.get("physviz").asBoolean()));
		gravity(new Vector3f(0, 0, -json.get("gravity").asFloat()));

		float[] ac = json.get("ambientColor").asFloatArray();
		ambientLight(new Color(ac[0], ac[1], ac[2], 1));

		if (!clearColorDefaultSet) {
			float[] cc = json.get("clearColor").asFloatArray();
			Bdx.display.clearColor.set(cc[0], cc[1], cc[2], 0);
			clearColorDefaultSet = true;
		}

		if (json.get("framerateProfile").asBoolean()){
			Bdx.profiler.visible(true);
		}

		float[] fc = json.get("clearColor").asFloatArray();
		fogColor = new Color(fc[0], fc[1], fc[2], 1);
		fog(json.get("mistOn").asBoolean());
		fogRange(json.get("mistStart").asFloat(), json.get("mistDepth").asFloat());

		for (JsonValue mat : json.get("materials")){
			String texName = mat.get("texture").asString();
			boolean hasAlpha = mat.get("alpha_blend").asString().equals("ALPHA");
			float opacity = hasAlpha ? mat.get("opacity").asFloat() : 1;

			Material material = new Material(mat.name);

			float[] c = mat.get("color").asFloatArray();
			material.set(ColorAttribute.createDiffuse(c[0], c[1], c[2], opacity));

			float[] s = mat.get("spec_color").asFloatArray();

			material.set(ColorAttribute.createSpecular(s[0], s[1], s[2], 1));

			material.set(FloatAttribute.createShininess(mat.get("shininess").asFloat()));

			material.set(new BDXColorAttribute(BDXColorAttribute.Tint, 0, 0, 0));

			IntAttribute shadeless = (IntAttribute) new BDXIntAttribute();

			if (mat.get("shadeless").asBoolean())
				shadeless.value = 1;

			material.set(shadeless);

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
				texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
				material.texture(texture);
			}

			material.set(new DepthTestAttribute());

			if (hasAlpha){
				BlendingAttribute ba = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
				ba.opacity = opacity;
				material.set(ba);
				material.set(FloatAttribute.createAlphaTest(0.01f));	// Discard pixels that fail this alpha test (sub-1% alpha)
				material.backToFrontSorting(true);						// Turn on back-to-front sorting for alpha-enabled objects by default
			}else{
				BlendingAttribute ba = new BlendingAttribute();
				ba.blended = false;
				material.set(ba);
			}

			materials.put(mat.name, material);
		}
		
		for (JsonValue model: json.get("models")){
			Mesh m = new Mesh(model, this);
			m.autoDispose = false;
			meshes.put(m.name, m);
		}

		HashMap<String, JsonValue> fonts = new HashMap<>();
		for (JsonValue fontj: json.get("fonts")){
			String font = fontj.asString();
			fonts.put(font, new JsonReader().parse(Gdx.files.internal("bdx/fonts/"+font+".fntx")));
		}

		FAnim.loadActions(json.get("actions"));

		for (JsonValue gobj: json.get("objects")){
			GameObject g = instantiator.newObject(gobj);
			g.json = gobj;
			g.name = gobj.name;
			g.scene = this;
			g.props = new HashMap<String, JsonValue>();
			for (JsonValue prop : gobj.get("properties")){
				g.props.put(prop.name, prop);
			}
						
			String meshName = gobj.get("mesh_name").asString();
			if (meshName != null){
				g.visibleNoChildren(gobj.get("visible").asBoolean());
				g.mesh(meshName);
			}else{
				g.visibleNoChildren(false);
				g.mesh(defaultMesh);
			}

			g.flipState(new Vector3f(gobj.get("scale").asFloatArray()));
			Matrix4f trans = new Matrix4f(gobj.get("transform").asFloatArray());
			trans.mul(g.flipState);
			g.transform.set(trans);
			JsonValue physics = gobj.get("physics");
			
			g.bodyType = BodyType.valueOf(physics.get("body_type").asString());
			g.boundsType = BoundsType.valueOf(physics.get("bounds_type").asString());
			g.body = Bullet.makeBody(g.mesh(), g.bodyType, g.boundsType, physics);
			g.body.setUserPointer(g);
			Bullet.updateBody(g);

			String type = gobj.get("type").asString();
			if (type.equals("FONT")){
				Text t = (Text)g;
				t.font = fonts.get(gobj.get("font").asString());
				String txt = gobj.get("text").asString();
				t.capacity = txt.length();
				t.text(txt);

				String align = gobj.get("alignment").asString();

				if (align.equals("RIGHT"))
					t.alignment(Text.Alignment.RIGHT);
				else if (align.equals("CENTER"))
					t.alignment(Text.Alignment.CENTER);
				else
					t.alignment(Text.Alignment.LEFT);

			}else if (type.equals("LAMP")){
				JsonValue settings = gobj.get("lamp");
				Light l = (Light)g;

				if (settings.getString("type").equals("SUN"))
					l.type = Light.Type.SUN;
				else if (settings.getString("type").equals("SPOT"))
					l.type = Light.Type.SPOT;
				else // POINT lamps; HEMI and AREA aren't supported, so they're turned into POINTs
					l.type = Light.Type.POINT;

				l.energy(settings.getFloat("energy"));
				float[] c = settings.get("color").asFloatArray();
				l.color(new Color(c[0], c[1], c[2], c[3]));

				if (l.type.equals(Light.Type.SPOT)) {
					l.spotSize(settings.getFloat("spot_size"));
				}
			}else if (type.equals("CAMERA")){
				Camera c = (Camera)g;
				float[] projection = gobj.get("camera").get("projection").asFloatArray();
				Vector2f resolution = new Vector2f(json.get("resolution").asFloatArray());
				if (gobj.get("camera").get("type").asString().equals("PERSP")){
					c.initData(Camera.Type.PERSPECTIVE);
					c.size(resolution);
					c.resolution(resolution);
					c.projection(new Matrix4f(projection));
					c.fov(c.fov());
				}else{
					c.initData(Camera.Type.ORTHOGRAPHIC);
					c.size(resolution);
					c.resolution(resolution);
					c.zoom(2 / projection[0]);
				}
				Matrix4 pm = new Matrix4(projection);
				pm.inv();
				Vector3 vec = new Vector3(0, 0, -1);
				vec.prj(pm);
				c.near(-vec.z);
				vec.set(0, 0, 1);
				vec.prj(pm);
				c.far(-vec.z);
			}
			
			templates.put(g.name, g);
		}

		hookParentChild();
		
		cameras = new ArrayListNamed<Camera>();
		
		addInstances();
		
		camera = (Camera) objects.get(json.get("cameras").asStringArray()[0]);
		String frameType = json.get("frame_type").asString();
		Viewport.Type viewportType;
		if (frameType.equals("LETTERBOX")){
			viewportType = Viewport.Type.LETTERBOX;
		}else if (frameType.equals("EXTEND")){
			viewportType = Viewport.Type.EXTEND;
		}else{ // "SCALE"
			viewportType = Viewport.Type.SCALE;
		}
		viewport = new Viewport(this, viewportType);
		
		for (GameObject g : sortByPriority(new ArrayList<GameObject>(objects))){
			initGameObject(g);
		}

		valid = true;
	}

	public void dispose(){

		valid = false;

		for (GameObject g : objects)
			g.endNoChildren();

		removeObjects();

		lastFrameBuffer.dispose();
		lastFrameBuffer = null;

		defaultMesh.dispose();
		defaultMesh = null;

		for (Texture t : textures.values())
			t.dispose();
		textures.clear();

		for (ScreenShader s : screenShaders)
			s.dispose();
		screenShaders.clear();

		for (Camera c : cameras) {
			if (c.renderBuffer != null)
				c.renderBuffer.dispose();
			c.renderBuffer = null;
		}

		for (Material m : materials.values())
			m.dispose();
		materials.clear();

		for (Mesh m : meshes.values())
			m.dispose();
		meshes.clear();

		for (Mesh m : new ArrayList<Mesh>(meshCopies))
			m.dispose();
		meshCopies.clear();

		world.destroy();
		world = null;

	}
	
	private void hookParentChild(){
		for (GameObject g : templates.values()){
			String parentName = g.json.get("parent").asString();
			if (parentName != null){
				g.parent(templates.get(parentName));
			}
		}
	}
	
	private ArrayList<GameObject> sortByPriority(ArrayList<GameObject> objects){
		for (GameObject g : objects){
			if (g.json.get("use_priority").asBoolean()){
				Collections.swap(objects, 0, objects.indexOf(g));
				break;
			}
		}
		return objects;
	}

	private void addInstances(){
		for (GameObject gobj : new ArrayList<GameObject>(templates.values())){
			if (gobj.json.get("active").asBoolean() && gobj.parent() == null){
				GameObject g = clone(gobj);
				addToWorld(g);
			}
		}
		for (GameObject g : toBeAdded){
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
		
		g.scene = this;
		
		g.mesh(gobj.mesh());

		g.body = Bullet.cloneBody(gobj.body);
		g.bodyType = gobj.bodyType;
		g.boundsType = gobj.boundsType;
		g.body.setUserPointer(g);
		g.transform(gobj.transform);
		g.flipState.set(gobj.flipState);
		
		g.props = new HashMap<String, JsonValue>(gobj.props);

		if (g instanceof Camera){
			Camera c = (Camera)g;
			Camera cobj = (Camera)gobj;
			c.initData(cobj.type);
			c.size(cobj.size());
			c.resolution(cobj.resolution());
			if (c.type == Camera.Type.PERSPECTIVE){
				c.fov(cobj.fov());
			}else{
				c.zoom(cobj.zoom());
			}
			c.near(cobj.near());
			c.far(cobj.far());
			c.update();
			cameras.add(c);
		}else if (g instanceof Text){
			Text t = (Text)g;
			Text tt = (Text)gobj;
			t.font = tt.font;
			t.capacity = tt.capacity;
			t.text(tt.text());
			t.mesh(t.mesh().copy());
			t.alignment(tt.alignment());
		}else if (g instanceof Light){
			Light l = (Light)g;
			Light ll = (Light)gobj;
			l.energy(ll.energy());
			l.color(ll.color());
			l.spotSize(ll.spotSize());
			l.exponent(ll.exponent());
			l.type = ll.type;
			l.makeLightData();
			l.updateLight();
			l.on(true);

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
			
			Matrix4f t = inst.transform.mult(g.transform);
			t.position(inst.position());
			g.transform(t);
			g.flipState.scale(inst.flipState);
			
			g.props.putAll(inst.props);

			for (GameObject c : inst.children){
				GameObject nc = clone(c);
				nc.parent(g);
			}
		}

		return g;

	}
	
	private void initGameObject(GameObject gobj){
		if (!gobj.initialized) {
			gobj.init();
			gobj.initialized = true;
		}
	}

	private void addToWorld(GameObject gobj){
		if (gobj.bodyType != BodyType.NO_COLLISION){
			world.addRigidBody(gobj.body, gobj.json.get("physics").get("group").asShort(), gobj.json.get("physics").get("mask").asShort());
			if (gobj.bodyType == BodyType.STATIC)
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
		GameObject p = clone(gobj);
		addToWorld(p);
		if (gobj.children.isEmpty()){
			initGameObject(p);
		}else{
			ArrayList<GameObject> parentAndChildren = new ArrayList<GameObject>();
			parentAndChildren.add(p);
			parentAndChildren.addAll(p.childrenRecursive());
			parentAndChildren = sortByPriority(parentAndChildren);
			for (GameObject g : parentAndChildren){
				initGameObject(g);
			}
		}
		return p;
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
		toBeRemoved.add(g);
	}

	public RayHit ray(Vector3f src, Vector3f vec, String... props){
		return ray(src, vec, (short)~0, (short)~0, props);
	}
	
	public RayHit ray(Vector3f src, Vector3f vec, short group, short mask, String... props){
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
		rh.normal = rrc.hitNormalWorld.normalized();

		for (String p : props){
			if (!rh.object.props.containsKey(p))
				return null;
		}
		
		return rh;
	}

	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec, boolean includeAll, String... props){
		return xray(src, vec, includeAll, (short)~0, (short)~0, props);
	}
	
	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec, boolean includeAll, short group, short mask, String... props){

		Vector3f startPos = new Vector3f(src);
		Vector3f dist = new Vector3f(vec);

		ArrayList<RayHit> hits = new ArrayList<RayHit>();
		ArrayList<GameObject> hitObjects = new ArrayList<GameObject>();

		boolean finished = false;

		while (!finished){

			RayHit ray = ray(startPos, dist, group, mask);

			if (ray != null){

				boolean skip = false;
				for (String prop : props) {
					if (!ray.object.props.containsKey(prop)) {
						skip = true;
						break;
					}
				}

				if (!skip && (!hitObjects.contains(ray.object) || includeAll)) {

					hits.add(ray);
					hitObjects.add(ray.object);

				}

				float delta = ray.position.minus(startPos).length();

				delta = Math.max(0.005f, delta);

				Vector3f n = new Vector3f(vec);
				n.length(delta);

				startPos.add(n);
				dist.sub(n);

			}
			else
				finished = true;

		}

		return hits;

	}

	public ArrayList<RayHit> xray(Vector3f src, Vector3f vec, String... props) {
		return xray(src, vec, false, props);
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
	
	private void updateVisuals(){
		Matrix4f m = new Matrix4f();
		float[] fa = new float[16];
		
		for (GameObject g : objects){
			if (g.visible()){
				m.set(g.transform);
				m.mul(g.flipState);
				m.transpose();
				m.get(fa);
				g.modelInstance.transform.set(fa);
			}
		}
	}
	
	private void runObjectLogic(){

		Bdx.mouse.init(this);

		for (Finger f : Bdx.fingers){
			f.init(this);
		}

		for (GameObject g : objects){

			if (g.bodyType() == BodyType.SENSOR)
				g.body.activate(true);

			if(!g.valid())
				continue;
			if (g instanceof Light)
				((Light) g).updateLight();
			for (Component c : g.components){
				if (c.state != null) {
					if (c.logicCounter >= 1) {
						if (Bdx.substepIndex() == Bdx.substepCount() - 1)
							c.logicCounter -= 1;
						c.state.main();
					}
					if (Bdx.substepIndex() == Bdx.substepCount() - 1)
						c.logicCounter += c.logicFrequency * Bdx.rawDelta();
				}
			}
			if (g.logicCounter >= 1) {
				if (Bdx.substepIndex() == Bdx.substepCount() - 1)
					g.logicCounter -= 1;
				g.main();
			}
			if (Bdx.substepIndex() == Bdx.substepCount() - 1)
				g.logicCounter += g.logicFrequency * Bdx.rawDelta();
		}

		for (GameObject g : toBeAdded) {
			objects.add(g);
			if (g instanceof Light)
				lights.add((Light) g);
		}
		toBeAdded.clear();

		removeObjects();

	}

	private void removeObjects() {

		for (GameObject g : toBeRemoved) {
			g.parent(null);
			if (g.body.isInWorld())
				world.removeRigidBody(g.body);
			g.body.setUserPointer(null);
			objects.remove(g);
			if (g instanceof Light)
				lights.remove(g);

			if (g.mesh().instances.size() == 0 && g.mesh().autoDispose)
				g.mesh().dispose();

			g.scene = null;
		}
		toBeRemoved.clear();

	}
	
	private void updateTransforms(){
		
		// update transforms of active dynamic objects without a parent
		
		Transform t = new Transform();
		Matrix4f m = new Matrix4f();
		Vector3f cmo = new Vector3f();
		Matrix4f cmt = new Matrix4f();
		
		for (GameObject g : objects){
			
			if (g.parent() == null && g.dynamics() && g.body.isActive()){
				
				// get unscaled transform from body
				
				g.body.getWorldTransform(t);
				t.getMatrix(m);
				
				// if off-center and primitive shape, get center of mass transform
				
				Vector3f med = g.mesh().median;
				if (med.length() != 0 && g.boundsType() != BoundsType.TRIANGLE_MESH && g.boundsType() != BoundsType.CONVEX_HULL){
					cmo.set(-med.x, -med.y, -med.z);
					cmt.set(cmo);
					m.mul(cmt);
				}
				
				// get and apply scaled transform
				
				m.scale(g.scale());
				g.transform.set(m);
				
				// update transforms of children
				
				g.updateChildTransforms();
			}
		}
	}

	public void ambientLight(Color color) {
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		ca.color.set(color);
	}
	
	public Color ambientLight(){
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		return new Color(ca.color);
	}

	public void update(){

		if (!valid)
			return;

		if (!paused){

			Bdx.profiler.start("__logic");
			runObjectLogic();
			Bdx.profiler.stop("__logic");

			updateVisuals();
			for (Camera cam : cameras) {
				if (cam == camera || cam.renderToTexture)		// Update camera if it's the main scene camera, or if it's rendering to texture
					cam.update();
			}
			Bdx.profiler.stop("__scene");

			try{
				if (Bdx.maxSubsteps() > 1)
					world.stepSimulation(Bdx.delta() * Bdx.physicsSpeed, 0, Bdx.TICK_TIME * Bdx.physicsSpeed / Bdx.substepCount());
				else
					world.stepSimulation(Bdx.TICK_TIME * Bdx.physicsSpeed, 0, Bdx.TICK_TIME);
			}catch (NullPointerException e){
				throw new RuntimeException("PHYSICS ERROR: Detected collision between Static objects set to Ghost, with Triangle Mesh bounds: Keep them separated, or use different bounds.");
			}
			Bdx.profiler.stop("__physics");

			updateTransforms();
			
			Bdx.profiler.stop("__scene");

			detectCollisions();
			Bdx.profiler.stop("__physics");

		}

		if (requestedEnd) {
			dispose();

			if (Bdx.scenes.contains(this))
				Bdx.scenes.remove(this);

		} else if (requestedRestart) {
			dispose();
			init();
		}

	}

	public void end(){
		requestedEnd = true;
	}

	public String toString(){

		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());

	}

	public void drawLine(Vector3f start, Vector3f end, Color color){
		ArrayList<Object> commands = new ArrayList<Object>();
		commands.add("drawLine");
		commands.add(color);
		commands.add(start);
		commands.add(end);
		drawCommands.add(commands);
	}

	public void drawPoint(Vector3f point, Color color){
		ArrayList<Object> commands = new ArrayList<Object>();
		commands.add("drawPoint");
		commands.add(color);
		commands.add(point);
		drawCommands.add(commands);
	}

	public void executeDrawCommands(){

		for (ArrayList<Object> commands : drawCommands) {

			String func = (String) commands.get(0);
			Color color = (Color) commands.get(1);
			Vector3f start = (Vector3f) commands.get(2);

			if (func.equals("drawLine")) {
				Vector3f end = (Vector3f) commands.get(3);
				shapeRenderer.setProjectionMatrix(camera.data.combined);
				shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
				shapeRenderer.setColor(color);
				shapeRenderer.line(start.x, start.y, start.z, end.x, end.y, end.z);
				shapeRenderer.end();
			}
			else {
				shapeRenderer.setProjectionMatrix(camera.data.combined);
				shapeRenderer.begin(ShapeRenderer.ShapeType.Point);
				shapeRenderer.setColor(color);
				shapeRenderer.point(start.x, start.y, start.z);
				shapeRenderer.end();
			}
		}

		drawCommands.clear();

	}

	public void fog(boolean fogOn){
		this.fogOn = fogOn;
		if (fogOn) {
			if (environment.get(ColorAttribute.Fog) == null)
				environment.set(new ColorAttribute(ColorAttribute.Fog, fogColor));
		}
		else {
			if (environment.get(ColorAttribute.Fog) != null)
				environment.remove(ColorAttribute.Fog);
		}
	}

	public boolean fog(){
		return fogOn;
	}

	public void fogColor(Color fogColor) {
		this.fogColor.set(fogColor);
		if (environment.get(ColorAttribute.Fog) != null) {
			ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.Fog);
			ca.color.set(fogColor);
		}
	}

	public Color fogColor(){
		return new Color(fogColor);
	}

	public void fogRange(float fogStart, float fogDepth){
		this.fogStart = fogStart;
		this.fogDepth = fogDepth;
	}

	public void fogRange(Vector2f range) {
		fogRange(range.x, range.y);
	}

	public Vector2f fogRange(){
		return new Vector2f(fogStart, fogDepth);
	}

	public boolean valid(){
		return valid;
	}

	public void debugPhysicsVisualization(boolean on){
		((Bullet.DebugDrawer) world.getDebugDrawer()).debug = on;
	}

	public boolean debugPhysicsVisualization(){
		return ((Bullet.DebugDrawer) world.getDebugDrawer()).debug;
	}

}
