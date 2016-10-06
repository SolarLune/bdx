package com.solarlune.fontwriter;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;

import java.util.ArrayList;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "FontWriter";
		config.width = 666;
		config.height = 444;

		ArrayList<String[]> files = new ArrayList<>();

		for (String a : arg)
			files.add(a.split("---"));

		new LwjglApplication(new ApplicationAdapter() {
			
			public void create() {

				for (String[] commands : files) {
					
					boolean properlyCreated = false;
					
					int resX = 256;
					int resY = 256;
					
					while(!properlyCreated) {
						
						String inputFontPath = commands[0];        // Supply the full path
						FileHandle outputFolder = Gdx.files.absolute(commands[1]);
						String fileName = outputFolder.nameWithoutExtension();
						outputFolder = outputFolder.parent();
						
						int fontSize = Integer.valueOf(commands[2]);
						int shadowOffsetX = Integer.valueOf(commands[3]);
						int shadowOffsetY = Integer.valueOf(commands[4]);
						Color shadowColor = new Color(Float.valueOf(commands[5]), Float.valueOf(commands[6]), Float.valueOf(commands[7]), Float.valueOf(commands[8]));
						int outlineThickness = Integer.valueOf(commands[9]);
						Color outlineColor = new Color(Float.valueOf(commands[10]), Float.valueOf(commands[11]), Float.valueOf(commands[12]), Float.valueOf(commands[13]));
						boolean outlineRounded = Boolean.valueOf(commands[14]);
						
						BitmapFontWriter.FontInfo fontInfo = new BitmapFontWriter.FontInfo();
						
						fontInfo.padding = new BitmapFontWriter.Padding(1, 1, 1, 1);
						
						FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(inputFontPath));
						FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
						
						parameter.color = new Color(Float.valueOf(commands[15]), Float.valueOf(commands[16]), Float.valueOf(commands[17]), Float.valueOf(commands[18]));
						parameter.borderColor = outlineColor;
						parameter.borderWidth = outlineThickness;
						parameter.borderStraight = !outlineRounded;
						parameter.shadowOffsetX = shadowOffsetX;
						parameter.shadowOffsetY = shadowOffsetY;
						parameter.shadowColor = shadowColor;
						parameter.size = fontSize;
						
						parameter.packer = new PixmapPacker(resX, resY, Pixmap.Format.RGBA8888, 2, false, new PixmapPacker.SkylineStrategy());
						FreeTypeFontGenerator.FreeTypeBitmapFontData data = generator.generateData(parameter);
						
						BitmapFontWriter.writeFont(data, new String[]{fileName + ".png"}, outputFolder.child(fileName + ".fnt"), fontInfo, resX, resY);    // Writes the .fnt file, I guess
						BitmapFontWriter.writePixmaps(parameter.packer.getPages(), outputFolder, fileName);
						
						generator.dispose();
						
						FileHandle path = outputFolder.child(fileName + ".png");
						
						if (path.exists())
							properlyCreated = true;
						else {                          // BitmapFontWriter generated multiple bitmaps; can't use them.
							resX *= 2;                  // We re-run with higher source texture res so they should fit
							resY *= 2;                  // on a single texture, continuously as necessary.
							for (FileHandle f : outputFolder.list()) {
								if (f.nameWithoutExtension().contains(fileName + "_") && f.extension().equals("png"))
									f.delete();
							}
						}
						
					}

				}
				
				Gdx.app.exit();

			}
			
		}, config);

	}

}
