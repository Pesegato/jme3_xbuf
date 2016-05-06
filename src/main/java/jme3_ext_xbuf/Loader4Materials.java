package jme3_ext_xbuf;

import static jme3_ext_xbuf.Converters.cnv;

import java.util.Map;

import org.slf4j.Logger;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;

import lombok.Getter;
import lombok.Setter;
import xbuf.Datas.Data;
import xbuf.Materials;
import xbuf.Primitives;
import xbuf.Primitives.Color;
import xbuf.Primitives.Texture2DInline;

public class Loader4Materials{
	protected final AssetManager assetManager;
	protected @Setter @Getter Texture defaultTexture;
	protected @Setter @Getter Material defaultMaterial;

	protected final MaterialReplicator materialReplicator;

	public Loader4Materials(AssetManager assetManager, MaterialReplicator materialReplicator) {
		this.assetManager = assetManager;
		this.materialReplicator = materialReplicator !=null?materialReplicator: new MaterialReplicator();
		defaultTexture = newDefaultTexture();
		defaultMaterial = newDefaultMaterial();
	}
	
	public Material newDefaultMaterial() {
		new Exception().printStackTrace();
		Material m=new Material(assetManager,"MatDefs/MatCap.j3md");
		m.setTexture("DiffuseMap",assetManager.loadTexture("Textures/generator8.jpg"));
		m.setColor("Multiply_Color",ColorRGBA.Pink);
		m.setFloat("ChessSize",0.5f);
		m.setName("DEFAULT");
		return m;
	}

	public Texture newDefaultTexture() {
		Texture t=assetManager.loadTexture("Textures/debug_8_64.png");
		t.setWrap(WrapMode.Repeat);
		t.setMagFilter(MagFilter.Nearest);
		t.setMinFilter(MinFilter.NearestLinearMipMap);
		t.setAnisotropicFilter(2);
		return t;
	}

	public Material newMaterial(Materials.Material m, Logger log) {
		boolean lightFamily=!m.getShadeless();
		String def=lightFamily?"Common/MatDefs/Light/Lighting.j3md":"Common/MatDefs/Misc/Unshaded.j3md";
		return new Material(assetManager,def);
	}

	public String findMaterialParamName(String[] names, VarType type, MaterialDef scope, Logger log) {
		for(String name2:names){
			for(MatParam mp:scope.getMaterialParams()){
				if(mp.getVarType()==type&&mp.getName().equalsIgnoreCase(name2)){ return mp.getName(); }
			}
		}
		return null;
	}

	public void setColor(boolean has, Color src, Material dst, String[] names, MaterialDef scope, Logger log) {
		if(has){
			String name=findMaterialParamName(names,VarType.Vector4,scope,log);
			if(name!=null){
				dst.setColor(name,cnv(src,new ColorRGBA()));
			}else{
				log.warn("can't find a matching name for : [{}] ({})",",",new Object[]{names,VarType.Vector4});
			}
		}
	}

	public void setBoolean(boolean has, Boolean src, Material dst, String[] names, MaterialDef scope, Logger log) {
		if(has){
			String name=findMaterialParamName(names,VarType.Boolean,scope,log);
			if(name!=null){
				dst.setBoolean(name,src);
			}else{
				log.warn("can't find a matching name for : [{}] ({})",",",new Object[]{names,VarType.Vector4});
			}
		}
	}

	public Texture getValue(Primitives.Texture t, Logger log) {
		Texture tex;
		switch(t.getDataCase()){
			case DATA_NOT_SET:
				tex=null;
				break;
			case RPATH:
				try{
					tex=assetManager.loadTexture(t.getRpath());
				}catch(AssetNotFoundException ex){
					log.warn("failed to load texture:",t.getRpath(),ex);
					tex=defaultTexture.clone();
				}
				break;
			case TEX2D:{
				Texture2DInline t2di=t.getTex2D();
				Image img=new Image(getValue(t2di.getFormat(),log),t2di.getWidth(),t2di.getHeight(),t2di.getData().asReadOnlyByteBuffer());
				tex=new Texture2D(img);
				break;
			}
			default:
				throw new IllegalArgumentException("doesn't support more than texture format:"+t.getDataCase());
		}
		tex.setWrap(WrapMode.Repeat);
		return tex;
	}

	public Image.Format getValue(Texture2DInline.Format f, Logger log) {
		switch(f){
			// case bgra8: return Image.Format.BGR8;
			case rgb8:
				return Image.Format.RGB8;
			case rgba8:
				return Image.Format.RGBA8;
			default:
				throw new UnsupportedOperationException("image format :"+f);
		}
	}

	public void setTexture2D(boolean has, Primitives.Texture src, Material dst, String[] names, MaterialDef scope, Logger log) {
		if(has){
			String name=findMaterialParamName(names,VarType.Texture2D,scope,log);
			if(name!=null){
				dst.setTexture(name,getValue(src,log));
			}else{
				log.warn("can't find a matching name for : [{}] ({})",",",new Object[]{names,VarType.Texture2D});
			}
		}
	}

	public void setFloat(boolean has, float src, Material dst, String[] names, MaterialDef scope, Logger log) {
		if(has){
			String name=findMaterialParamName(names,VarType.Float,scope,log);
			if(name!=null){
				dst.setFloat(name,src);
			}else{
				log.warn("can't find a matching name for : [{}] ({})",",",new Object[]{names,VarType.Float});
			}
		}
	}

	public Material mergeToMaterial(Materials.Material src, Material dst, Logger log) {
		MaterialDef md=dst.getMaterialDef();
		setColor(src.hasColor(),src.getColor(),dst,new String[]{"Color","Diffuse"},md,log);
		setTexture2D(src.hasColorMap(),src.getColorMap(),dst,new String[]{"ColorMap","DiffuseMap"},md,log);
		// setTexture2D(src.hasNormalMap(), src.getNormalMap(), dst, new String[]{"ColorMap", "DiffuseMap"], md, log)
		setFloat(src.hasOpacity(),src.getOpacity(),dst,new String[]{"Alpha","Opacity"},md,log);
		setTexture2D(src.hasOpacityMap(),src.getOpacityMap(),dst,new String[]{"AlphaMap","OpacityMap"},md,log);
		setTexture2D(src.hasNormalMap(),src.getNormalMap(),dst,new String[]{"NormalMap"},md,log);
		setFloat(src.hasRoughness(),src.getRoughness(),dst,new String[]{"Roughness"},md,log);
		setTexture2D(src.hasRoughnessMap(),src.getRoughnessMap(),dst,new String[]{"RoughnessMap"},md,log);
		setFloat(src.hasMetalness(),src.getMetalness(),dst,new String[]{"Metalness"},md,log);
		setTexture2D(src.hasMetalnessMap(),src.getMetalnessMap(),dst,new String[]{"MetalnessMap"},md,log);
		setColor(src.hasSpecular(),src.getSpecular(),dst,new String[]{"Specular"},md,log);
		setTexture2D(src.hasSpecularMap(),src.getSpecularMap(),dst,new String[]{"SpecularMap"},md,log);
		setFloat(src.hasSpecularPower(),src.getSpecularPower(),dst,new String[]{"SpecularPower","Shininess"},md,log);
		setTexture2D(src.hasSpecularPowerMap(),src.getSpecularPowerMap(),dst,new String[]{"SpecularPowerMap","ShininessMap"},md,log);
		setColor(src.hasEmission(),src.getEmission(),dst,new String[]{"Emission","GlowColor"},md,log);
		setTexture2D(src.hasEmissionMap(),src.getEmissionMap(),dst,new String[]{"EmissionMap","GlowMap"},md,log);
		if(!src.getShadeless()){
			if(!src.hasColorMap()){
				if(src.hasColor()){
					setBoolean(true,true,dst,new String[]{"UseMaterialColors"},md,log);
				}else{
					setBoolean(true,true,dst,new String[]{"UseVertexColor"},md,log);
				}
			}
		}
		return dst;
	}

	public void mergeMaterials(Data src, Map<String,Object> components, Logger log) {
		src.getMaterialsList().stream().forEach(m -> {
			String id=m.getId();
			Material mat=newMaterial(m,log);
			components.put(id,mat);
			mat.setName(m.hasName()?m.getName():m.getId());
			mergeToMaterial(m,mat,log);
//			materialReplicator.syncReplicas(mat);
		});

	}
}
