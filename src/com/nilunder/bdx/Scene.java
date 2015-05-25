package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.components.*;
import com.nilunder.bdx.Bdx;

public class Scene implements Named{
	public static HashMap<String, Instantiator> instantiators;

	public JsonValue json;

	public String name;
	public LinkedListNamed<GameObject> objects;
	public Camera camera;
	public ArrayList<Camera> cameras;
	public PerspectiveCamera cam;
	public HashMap<Model, Vector2f> modelToFrame;

	private FileHandle scene;

	public HashMap<String,Model> models;
	private HashMap<String,Texture> textures;
	private HashMap<String,Material> materials;
	public Material defaultMaterial;
	private Model defaultModel;
	public DiscreteDynamicsWorld world;

	private ArrayList<GameObject> toBeAdded;
	private ArrayList<GameObject> toBeRemoved;

	private boolean requestedRestart;
	public boolean paused;
	
	private Instantiator instantiator;
	
	private HashMap<String, GameObject> templates;
	public ArrayList<Filter> filters;
	public RenderBuffer lastFrameBuffer;
	public Environment environment;
	
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

	public void init(){
		requestedRestart = false;
		paused = false;

		lastFrameBuffer = new RenderBuffer(null);
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0, 0, 0, 1));
				
		filters = new ArrayList<Filter>();
		defaultMaterial = new Material();
		defaultMaterial.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1));
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
		templates = new HashMap<String, GameObject>();
		
		json = new JsonReader().parse(scene);
		name = json.get("name").asString();
		
		world = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
		world.setDebugDrawer(new Bullet.DebugDrawer(json.get("physviz").asBoolean()));
		gravity(new Vector3f(0, 0, -json.get("gravity").asFloat()));
		ambientLight(new Vector4f(json.get("ambientColor").asFloatArray()));

		Bdx.profiler.init(json.get("framerateProfile").asBoolean());

		for (JsonValue mat : json.get("materials")){
			String texName = mat.get("texture").asString();

			float[] c = mat.get("color").asFloatArray();
			Material material = new Material(ColorAttribute.createDiffuse(c[0], c[1], c[2], 1));
							
			if (mat.get("shadeless").asBoolean())
				material.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1));
			
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

			if (mat.get("alpha_blend").asString().equals("ALPHA")) {
				BlendingAttribute ba = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
				ba.opacity = mat.get("opacity").asFloat();
				material.set(ba);
				material.set(FloatAttribute.createAlphaTest(0));
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
				l.energy(settings.getFloat("energy"));
				l.color(new Vector4f(settings.get("color").asFloatArray()));
				l.type = settings.getString("type");
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
			float[] trans = gobj.get("transform").asFloatArray();
			
			g.body = Bullet.makeBody(g.modelInstance.model.meshes.first(), trans, gobj.get("physics"));
			g.currBodyType = gobj.get("physics").get("body_type").asString();
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
			cameras.add((Camera)objects.get(cn));
		camera = cameras.get(0);

		cam = new PerspectiveCamera();
		cam.projection.set(camera.json.get("camera").get("projection").asFloatArray());

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
			if (t.name.contains("init_")){
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
		objects.addAll(toBeAdded);
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
		g.body.setUserPointer(g);
		g.scale(gobj.scale());
		
		g.props = gobj.props;
		
		g.scene = this;

		if (g instanceof Camera){
			Camera c = (Camera)g;
			Camera cc = (Camera)gobj;
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
			l.type = ll.type;
			l.makeLightData();
			environment.add(l.lightData);
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
		if (!gobj.currBodyType.equals("NO_COLLISION"))
			world.addRigidBody(gobj.body, gobj.json.get("physics").get("group").asShort(), gobj.json.get("physics").get("mask").asShort());
		
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
		int part_idx = 0;
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
			++part_idx;
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
			ArrayListNamed<GameObject> hitLast = g.touchingObjectsLast;
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
		Bdx.mouse.scene = this;

		for (Finger f : Bdx.fingers){
			f.scene = this;
		}

		for (GameObject g : objects){
			if(!g.valid())
				continue;
			for (Component c : g.components){
				if (c.state != null)
					c.state.main();
			}
			g.main();
		}
		if (toBeAdded.size() > 0){
			objects.addAll(toBeAdded);
			toBeAdded.clear();
		}
		if (toBeRemoved.size() > 0){
			objects.removeAll(toBeRemoved);
			toBeRemoved.clear();
		}
		if (requestedRestart){
			for (Model m : models.values()){
				m.dispose();
			}
			for (Texture t : textures.values()){
				t.dispose();
			}
			init();
		}
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
		cam.position.set(t.origin.x, t.origin.y, t.origin.z);
		t.inverse();
		t.getOpenGLMatrix(m);
		cam.view.set(m);
		cam.combined.set(cam.projection);
		Matrix4.mul(cam.combined.val, cam.view.val);

		// Frustum 
		cam.invProjectionView.set(cam.combined);
		Matrix4.inv(cam.invProjectionView.val);
		cam.frustum.update(cam.invProjectionView);

	}
	
	public void ambientLight(Vector4f color){
		ambientLight(color.x, color.y, color.z, color.w);
	}
	
	public void ambientLight(float r, float g, float b, float a) {
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		ca.color.set(r, g, b, a);
	}
	
	public Vector4f ambientLight(){
		ColorAttribute ca = (ColorAttribute) environment.get(ColorAttribute.AmbientLight);
		return new Vector4f(ca.color.r, ca.color.g, ca.color.b, ca.color.a);
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

}
