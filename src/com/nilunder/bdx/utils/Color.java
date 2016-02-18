package com.nilunder.bdx.utils;

public class Color extends com.badlogic.gdx.graphics.Color {

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
