package com.nilunder.bdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.vecmath.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
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

public class Scene implements Named{
	public static HashMap<String, Instantiator> instantiators;

	public String name;
	public LinkedListNamed<GameObject> objects;
	public Camera camera;
	public ArrayList<Camera> cameras;
	public PerspectiveCamera cam;
	public HashMap<Model, Vector2f> modelToFrame;

	private FileHandle scene;

	private HashMap<String,Model> models;
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

	public String name(){
		return name;
	}

	public void init(){
		requestedRestart = false;
		paused = false;

		
		defaultMaterial = new Material();
		defaultModel = new ModelBuilder().createBox(1.0f, 1.0f, 1.0f, defaultMaterial, Usage.Position | Usage.TextureCoordinates);

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
		
		JsonValue json = new JsonReader().parse(scene);
		name = json.get("name").asString();
		
		world = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
		world.setGravity(new Vector3f(0, 0, -json.get("gravity").asFloat()));

		for (JsonValue mat : json.get("materials")){
			String texName = mat.get("texture").asString();

			float[] c = mat.get("color").asFloatArray();
			Material material = new Material(ColorAttribute.createDiffuse(c[0], c[1], c[2], 1));
							
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
			models.put(model.name, createModel(model.name, model));
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

			g.name = gobj.name;

			JsonValue mesh = gobj.get("model");
			String meshName = mesh.asString();
			if (meshName != null){
				g.visible = gobj.get("visible").asBoolean();
				g.modelInstance = new ModelInstance(models.get(meshName));
			}else{
				g.visible = false;
				g.modelInstance = new ModelInstance(defaultModel);
			}
			Mesh m = g.modelInstance.model.meshes.first();

			float[] trans = gobj.get("transform").asFloatArray();
			g.body = Bullet.makeBody(m, trans, gobj.get("physics"));
			g.body.setUserPointer(g);
			
			g.props = gobj.get("properties");
			
			g._json = gobj;
			
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
		cam.projection.set(camera._json.get("camera").get("projection").asFloatArray());

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

	private void hookParentChild(){
		for (GameObject g : templates.values()){
			String parentName = g._json.get("parent").asString();
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
			boolean onActiveLayer = gobj._json.get("active").asBoolean();
			if (onActiveLayer && gobj.parent() == null){
				GameObject g = clone(gobj);
				addToWorld(g);
			}
		}
		objects.addAll(toBeAdded);
		toBeAdded.clear();
	}
	

	private GameObject cloneNoChildren(GameObject gobj){
		GameObject g = instantiator.newObject(gobj._json);

		if (g instanceof Camera){
			Camera c = (Camera)g;
			Camera cc = (Camera)gobj;
		}else if (g instanceof Text){
			Text t = (Text)g;
			Text tt = (Text)gobj;
			t.font = tt.font;
		}

		g._json = gobj._json;
		
		g.name = gobj.name;
		g.visible = gobj.visible;
		g.modelInstance = new ModelInstance(gobj.modelInstance);
		
		g.body = Bullet.cloneBody(gobj.body);
		g.body.setUserPointer(g);
		g.scale(gobj.scale());
		
		g.props = gobj.props;
		
		g.scene = this;

		return g;
	}

	private GameObject clone(GameObject gobj){
		String instance = gobj._json.get("instance").asString();

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

			g.props = inst.props;

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
		boolean collisionEnabled = !gobj._json.get("physics").get("body").asString().equals("NO_COLLISION");
		if (collisionEnabled)
			world.addRigidBody(gobj.body);
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
		g.parent(null);
		world.removeRigidBody(g.body);
		toBeRemoved.add(g);
		g.valid = false;
	}
	
	public RayHit ray(Vector3f src, Vector3f vec){
		Vector3f to = new Vector3f(src);
		to.add(vec);
		
		CollisionWorld.ClosestRayResultCallback rrc = new CollisionWorld.ClosestRayResultCallback(src, to);
		
		world.rayTest(src, to, rrc);
		
		if (!rrc.hasHit())
			return null;
		
		RayHit rh = new RayHit();
		rh.object = (GameObject) (rrc.collisionObject.getUserPointer());
		rh.position = rrc.hitPointWorld;
		rh.normal = rrc.hitNormalWorld;
		
		return rh;
	}
	
	
	private Model createModel(String name, JsonValue model) {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		int part_idx = 0;
		short idx = 0;
		for (JsonValue mat : model){
			MeshPartBuilder mpb = builder.part(name + part_idx, GL20.GL_TRIANGLES, Usage.Position | Usage.TextureCoordinates, materials.get(mat.name));
			float verts[] = mat.asFloatArray();
			mpb.vertex(verts);
			int len = verts.length / 5;
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
			if (g.visible){
				g.body.getMotionState().getWorldTransform(trans);
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
			System.out.println("requestedRestart");
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
	
	
	public void update(){
		
		if (!paused){

			runObjectLogic();			
			
			updateVisuals();
			
			updateCamera();
			
			world.stepSimulation(Bdx.TICK_TIME, 0);
			
			updateChildBodies();
			
			detectCollisions();
	

			
		}

	}
}
