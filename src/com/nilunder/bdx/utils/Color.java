package com.nilunder.bdx.utils;

public class Color extends com.badlogic.gdx.graphics.Color {

	public static final Color CLEAR = new Color(com.badlogic.gdx.graphics.Color.CLEAR);
	public static final Color BLACK = new Color(com.badlogic.gdx.graphics.Color.BLACK);
	public static final Color WHITE = new Color(com.badlogic.gdx.graphics.Color.WHITE);
	public static final Color LIGHT_GRAY = new Color(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
	public static final Color GRAY = new Color(com.badlogic.gdx.graphics.Color.GRAY);
	public static final Color DARK_GRAY = new Color(com.badlogic.gdx.graphics.Color.DARK_GRAY);
	public static final Color BLUE = new Color(com.badlogic.gdx.graphics.Color.BLUE);
	public static final Color NAVY = new Color(com.badlogic.gdx.graphics.Color.NAVY);
	public static final Color ROYAL = new Color(com.badlogic.gdx.graphics.Color.ROYAL);
	public static final Color SLATE = new Color(com.badlogic.gdx.graphics.Color.SLATE);
	public static final Color SKY = new Color(com.badlogic.gdx.graphics.Color.SKY);
	public static final Color CYAN = new Color(com.badlogic.gdx.graphics.Color.CYAN);
	public static final Color TEAL = new Color(com.badlogic.gdx.graphics.Color.TEAL);
	public static final Color GREEN = new Color(com.badlogic.gdx.graphics.Color.GREEN);
	public static final Color CHARTREUSE = new Color(com.badlogic.gdx.graphics.Color.CHARTREUSE);
	public static final Color LIME = new Color(com.badlogic.gdx.graphics.Color.LIME);
	public static final Color FOREST = new Color(com.badlogic.gdx.graphics.Color.FOREST);
	public static final Color OLIVE = new Color(com.badlogic.gdx.graphics.Color.OLIVE);
	public static final Color YELLOW = new Color(com.badlogic.gdx.graphics.Color.YELLOW);
	public static final Color GOLD = new Color(com.badlogic.gdx.graphics.Color.GOLD);
	public static final Color GOLDENROD = new Color(com.badlogic.gdx.graphics.Color.GOLDENROD);
	public static final Color ORANGE = new Color(com.badlogic.gdx.graphics.Color.ORANGE);
	public static final Color BROWN = new Color(com.badlogic.gdx.graphics.Color.BROWN);
	public static final Color TAN = new Color(com.badlogic.gdx.graphics.Color.TAN);
	public static final Color FIREBRICK = new Color(com.badlogic.gdx.graphics.Color.FIREBRICK);
	public static final Color RED = new Color(com.badlogic.gdx.graphics.Color.RED);
	public static final Color SCARLET = new Color(com.badlogic.gdx.graphics.Color.SCARLET);
	public static final Color CORAL = new Color(com.badlogic.gdx.graphics.Color.CORAL);
	public static final Color SALMON = new Color(com.badlogic.gdx.graphics.Color.SALMON);
	public static final Color PINK = new Color(com.badlogic.gdx.graphics.Color.PINK);
	public static final Color MAGENTA = new Color(com.badlogic.gdx.graphics.Color.MAGENTA);
	public static final Color PURPLE = new Color(com.badlogic.gdx.graphics.Color.PURPLE);
	public static final Color VIOLET = new Color(com.badlogic.gdx.graphics.Color.VIOLET);
	public static final Color MAROON = new Color(com.badlogic.gdx.graphics.Color.MAROON);

	public Color() {super();}

	public Color(int rgba8888) {
		super(rgba8888);
	}

	public Color(float r, float g, float b, float a) {
		super(r, g, b, a);
	}

	public Color(com.badlogic.gdx.graphics.Color color) {
		super(color);
	}

	public Color setAlpha(float alphaValue){
		a = alphaValue;
		return clamp();
	}

	public Color subAlpha(float alphaValue){
		a -= alphaValue;
		return clamp();
	}

	public Color addAlpha(float alphaValue){
		a += alphaValue;
		return clamp();
	}

	public Color mulAlpha(float mulAmount){
		a *= mulAmount;
		return clamp();
	}

	public Color subColor(float value) {
		sub(value, value, value, 0);
		return clamp();
	}

	public Color addColor(float value) {
		add(value, value, value, 0);
		return clamp();
	}

	public Color mulColor(float mulAmount) {
		mul(mulAmount, mulAmount, mulAmount, 1);
		return clamp();
	}

	public Color HSV(float hue, float sat, float val){
		float i = (float) Math.floor(hue * 6.0f);
		float f = hue * 6 - i;
		float p = val * (1 - sat);
		float q = val * (1 - f * sat);
		float t = val * (1 - (1 - f) * sat);

		switch((int) i % 6){
			case 0: r = val; g = t; b = p; break;
			case 1: r = q; g = val; b = p; break;
			case 2: r = p; g = val; b = t; break;
			case 3: r = p; g = q; b = val; break;
			case 4: r = t; g = p; b = val; break;
			case 5: r = val; g = p; b = q; break;
		}

		return clamp();
	}

	public float[] HSV(){

		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));

		float h, s, v = max;

		float d = max - min;
		s = max == 0 ? 0 : d / max;

		if(max == min){
			h = 0; // achromatic
		}else{
			if (max == r)
				h = (g - b) / d + (g < b ? 6 : 0);
			else if (max == g)
				h = (b - r) / d + 2;
			else
				h = (r - g) / d + 4;
			h /= 6;
		}

		return new float[]{h, s, v};

	}

	public Color hue(float hue){
		float[] hsv = HSV();
		return HSV(hue, hsv[1], hsv[2]);
	}

	public float hue(){
		return HSV()[0];
	}

	public Color sat(float sat){
		float[] hsv = HSV();
		return HSV(hsv[0], sat, hsv[2]);
	}

	public float sat(){
		return HSV()[1];
	}

	public Color val(float val){
		float[] hsv = HSV();
		return HSV(hsv[0], hsv[1], val);
	}

	public float val(){
		return HSV()[2];
	}

	// Overriding GDX Functions with BDX ones that return the new Color class

	public Color set(com.badlogic.gdx.graphics.Color color) {
		super.set(color);
		return this;
	}

	public Color mul(com.badlogic.gdx.graphics.Color color) {
		super.mul(color);
		return this;
	}

	public Color mul(float value) {
		super.mul(value);
		return this;
	}

	public Color add(com.badlogic.gdx.graphics.Color color) {
		super.add(color);
		return this;
	}

	public Color sub(com.badlogic.gdx.graphics.Color color) {
		super.sub(color);
		return this;
	}

	public Color clamp() {
		super.clamp();
		return this;
	}

	public Color set(float r, float g, float b, float a) {
		super.set(r, g, b, a);
		return this;
	}

	public Color set(int rgba) {
		super.set(rgba);
		return this;
	}

	public Color add(float r, float g, float b, float a) {
		super.add(r, g, b, a);
		return this;
	}

	public Color sub(float r, float g, float b, float a) {
		super.sub(r, g, b, a);
		return this;
	}

	public Color mul(float r, float g, float b, float a) {
		super.mul(r, g, b, a);
		return this;
	}

	public Color lerp(com.badlogic.gdx.graphics.Color target, float t) {
		super.lerp(target, t);
		return this;
	}

	public Color lerp(float r, float g, float b, float a, float t) {
		super.lerp(r, g, b, a, t);
		return this;
	}

	public Color premultiplyAlpha() {
		super.premultiplyAlpha();
		return this;
	}

	public static Color valueOf(String hex) {
		return new Color(com.badlogic.gdx.graphics.Color.valueOf(hex));
	}

}
