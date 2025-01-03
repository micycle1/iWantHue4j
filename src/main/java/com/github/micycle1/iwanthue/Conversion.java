package com.github.micycle1.iwanthue;

class Conversion {

	static final double Kn = 18;

	// D65 standard referent
	static final double Xn = 0.950470;
	static final double Yn = 1;
	static final double Zn = 1.088830;

	static final double t0 = 0.137931034; // 4 / 29
	static final double t1 = 0.206896552; // 6 / 29
	static final double t2 = 0.12841855; // 3 * t1 * t1
	static final double t3 = 0.008856452; // t1 * t1 * t1

	public static boolean validateLab(double[] lab) {
		double[] rgb = labToRgb(lab);
		double R = rgb[0];
		double G = rgb[1];
		double B = rgb[2];
		return R >= 0 && R <= 255 && G >= 0 && G <= 255 && B >= 0 && B <= 255;
	}

	/**
	 * 
	 * @param lab
	 * @return rgb [0..255]
	 */
	public static double[] labToRgb(double[] lab) {
		// Code from Chroma.js 2016
		double l = lab[0];
		double a = lab[1];
		double b = lab[2];

		double y = (l + 16) / 116;
		double x = (Double.isNaN(a)) ? y : (y + a / 500);
		double z = (Double.isNaN(b)) ? y : (y - b / 200);

		y = Yn * labToXyz(y);
		x = Xn * labToXyz(x);
		z = Zn * labToXyz(z);

		double R = xyzToRgb(3.2404542 * x - 1.5371385 * y - 0.4985314 * z); // D65 -> sRGB
		double G = xyzToRgb(-0.9692660 * x + 1.8760108 * y + 0.0415560 * z);
		double B = xyzToRgb(0.0556434 * x - 0.2040259 * y + 1.0572252 * z);
		return new double[] { R, G, B };
	}

	private static double xyzToRgb(double r) {
		return (255 * ((r <= 0.00304) ? (12.92 * r) : (1.055 * Math.pow(r, 1 / 2.4) - 0.055)));
	}

	private static double labToXyz(double t) {
		return (t > t1) ? (t * t * t) : (t2 * (t - t0));
	}

	/**
	 * 
	 * @param rgb [0..255]
	 * @return
	 */
	public static double[] rgbToLab(double[] rgb) {
		// Convert RGB to XYZ
		double r = rgbToXyz(rgb[0]);
		double g = rgbToXyz(rgb[1]);
		double b = rgbToXyz(rgb[2]);

		double x = xyzToLab((0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / Xn);
		double y = xyzToLab((0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / Yn);
		double z = xyzToLab((0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / Zn);

		// Convert XYZ to Lab
		double L = 116 * y - 16;
		double a = 500 * (x - y);
		double b_ = 200 * (y - z);

		return new double[] { L, a, b_ };
	}

	private static double rgbToXyz(double c) {
		double c_norm = c / 255.0;
		return (c_norm > 0.04045) ? Math.pow((c_norm + 0.055) / 1.055, 2.4) : (c_norm / 12.92);
	}

	private static double xyzToLab(double t) {
		return (t > t3) ? Math.pow(t, 1 / 3.0) : (t / t2 + t0);
	}
}