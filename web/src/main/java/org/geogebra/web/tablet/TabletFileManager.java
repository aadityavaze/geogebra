package org.geogebra.web.tablet;

import java.util.ArrayList;

import org.geogebra.common.main.Feature;
import org.geogebra.common.main.MaterialsManager;
import org.geogebra.common.move.ggtapi.models.JSONParserGGT;
import org.geogebra.common.move.ggtapi.models.Material;
import org.geogebra.common.move.ggtapi.models.Material.MaterialType;
import org.geogebra.common.move.ggtapi.models.MaterialFilter;
import org.geogebra.common.move.ggtapi.models.SyncEvent;
import org.geogebra.common.move.ggtapi.models.json.JSONArray;
import org.geogebra.common.move.ggtapi.models.json.JSONException;
import org.geogebra.common.move.ggtapi.models.json.JSONObject;
import org.geogebra.common.move.ggtapi.models.json.JSONTokener;
import org.geogebra.common.util.debug.Log;
import org.geogebra.web.html5.main.AppW;
import org.geogebra.web.touch.FileManagerT;
import org.geogebra.web.web.util.SaveCallback;

public class TabletFileManager extends FileManagerT {

	public TabletFileManager(AppW tabletApp) {
		super(tabletApp);
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			exportJavascriptMethods();
		}
	}
	
	@Override
	protected void getFiles(final MaterialFilter filter) {
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			getFilesFilter = filter;
			getFilesNative();
		}else{
			super.getFiles(filter);
		}
	}
	
	private MaterialFilter getFilesFilter = null;
	
	private native void getFilesNative() /*-{
		if ($wnd.android) {
			$wnd.android.getMetaDatas();
		}
	}-*/;
	
	/**
	 * this method is called through js (see exportJavascriptMethods())
	 */
	public void catchMetaDatas(String data) {

		JSONTokener tokener = new JSONTokener(data);
		try {
			JSONArray arr = new JSONArray(tokener);
			String name = (String) arr.get(0);
			JSONObject metaDatas = (JSONObject) arr.get(1);
			Material mat = JSONParserGGT.prototype.toMaterial(metaDatas);

			if (mat == null) {
				mat = new Material(
						0,
						MaterialType.ggb);
				mat.setTitle(getTitleFromKey(name));
			}

			mat.setLocalID(MaterialsManager.getIDFromKey(name));

			if (getFilesFilter.check(mat)) {
				addMaterial(mat);
				Log.debug("add material: "+name+", id: "+mat.getLocalID());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	
	
	@Override
    public void openMaterial(final Material material) {
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			openMaterialMaterial = material;
			String fileName = getFileKey(material);
			Log.debug("openMaterial: "+fileName+", id: "+material.getLocalID());
			getBase64(fileName);
		}else{
			super.openMaterial(material);
		}
	}
	
	private Material openMaterialMaterial = null;
	
	private native void getBase64(String fileName) /*-{
		if ($wnd.android) {
			$wnd.android.getBase64(fileName);
		}
	}-*/;
	
	/**
	 * this method is called through js (see exportJavascriptMethods())
	 */
	public void catchBase64(String data) {
		openMaterialMaterial.setBase64(data);
		doOpenMaterial(openMaterialMaterial);
	}
	
	
	private SaveCallback saveCallback;
	private Material saveFileMaterial;	
	
	@Override
	public void saveFile(final String base64, final long modified,
			 final SaveCallback cb) {
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			saveCallback = cb;
			saveFileMaterial = createMaterial("", modified);
			saveFileMaterial.setBase64("");
			saveFileNative(getApp().getLocalID(), getTitleWithoutReservedCharacters(getApp()
			        .getKernel().getConstruction().getTitle()),base64, saveFileMaterial.toJson().toString());
		}else{
			super.saveFile(base64, modified, cb);
		}
	}
	
	/**
	 * this method is called through js (see exportJavascriptMethods())
	 */
	public void catchSaveFileResult(String idString) {
		if (idString == null || "0".equals(idString)){
			saveCallback.onError();
		}else{
			try{
				int id = Integer.parseInt(idString);
				saveFileMaterial.setLocalID(id);
				saveCallback.onSaved(saveFileMaterial, true);
			}catch(NumberFormatException e){
				Log.debug("error parsing material id: "+idString+", message: "+e.getMessage());
			}
		}
	}
			
	
	private native void saveFileNative(int id, String title, String base64, String metaDatas) /*-{
		if ($wnd.android) {
			$wnd.android.saveFile(id, title, base64, metaDatas);
		}
	}-*/;
	
	
	
	
	@Override
	public void uploadUsersMaterials(final ArrayList<SyncEvent> events) {
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			//TODO ?
			Log.debug("uploadUsersMaterials");
		} else {
			super.uploadUsersMaterials(events);
		}
		
	}
	
	@Override
	public void open(String url, String name, String features){
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			openUrlInBrowser(url, name, features);
		} else {
			super.open(url, name, features);
		}
	}
	
	@Override
	public void open(String url){
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			openUrlInBrowser(url, "", "");
		} else {
			super.open(url);
		}
	}
	
	private native void openUrlInBrowser(String url, String name, String features) /*-{
		if ($wnd.android) {
			$wnd.android.openUrlInBrowser(url, name, features);
		}
	}-*/;
	
	
	
	
	@Override
	public void rename(final String newTitle, final Material mat,
	        final Runnable callback) {
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			final String newKey = MaterialsManager.createKeyString(mat.getLocalID(),
					newTitle);
			final String oldKey = getFileKey(mat);
			mat.setBase64("");
			mat.setTitle(newTitle);
			renameNative(oldKey, newKey, mat.toJson().toString());
		} else {
			super.rename(newTitle, mat, callback);
		}		
	}
	
	private native void renameNative(String oldKey, String newKey, String metaData) /*-{
		if ($wnd.android) {
			$wnd.android.rename(oldKey, newKey, metaData);
		}
	}-*/;
		
	
	
	private Runnable deleteOnSuccess;
	private Material deleteMaterial;
	
	@Override
	public void delete(final Material mat, boolean permanent,
	        final Runnable onSuccess) {
		
		if (app.has(Feature.TABLET_WITHOUT_CORDOVA)){
			if (!permanent) {
				mat.setDeleted(true);
				mat.setBase64("");
				overwriteMetaDataNative(getFileKey(mat), mat.toJson().toString());
				return;
			}

			deleteMaterial = mat;
			deleteOnSuccess = onSuccess;		
			deleteNative(getFileKey(mat));
		}else{
			super.delete(mat, permanent, onSuccess);
		}

	}
	
	private native void overwriteMetaDataNative(String key, String metaData)/*-{
		if ($wnd.android) {
			$wnd.android.overwriteMetaData(key, metaData);
		}
	}-*/;
	
	private native void deleteNative(String key) /*-{
		if ($wnd.android) {
			$wnd.android.deleteGgb(key);
		}
	}-*/;
	
	/**
	 * this method is called through js (see exportJavascriptMethods())
	 */
	public void catchDeleteResult(String result) {
		if (result == null || "0".equals(result)){
			return;
		}		
		removeFile(deleteMaterial);
		deleteOnSuccess.run();
	}
	
	
	
	private native void exportJavascriptMethods() /*-{
		var that = this;
		$wnd.tabletFileManager_catchMetaDatas = $entry(function(data) {
			that.@org.geogebra.web.tablet.TabletFileManager::catchMetaDatas(Ljava/lang/String;)(data);
		});
		$wnd.tabletFileManager_catchBase64 = $entry(function(data) {
			that.@org.geogebra.web.tablet.TabletFileManager::catchBase64(Ljava/lang/String;)(data);
		});
		$wnd.tabletFileManager_catchSaveFileResult = $entry(function(data) {
			that.@org.geogebra.web.tablet.TabletFileManager::catchSaveFileResult(Ljava/lang/String;)(data);
		});
		$wnd.tabletFileManager_catchDeleteResult = $entry(function(data) {
			that.@org.geogebra.web.tablet.TabletFileManager::catchDeleteResult(Ljava/lang/String;)(data);
		});
	}-*/;
	

}
