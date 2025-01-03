package com.github.micycle.iwanthue;

class LabValidator {

	static final double Kn = 18;

	// D65 standard referent
	static final double Xn = 0.950470;
	static final double Yn = 1;
	static final double Zn = 1.088830;

	static final double t0 = 0.137931034; // 4 / 29
	static final double t1 = 0.206896552; // 6 / 29
	static final double t2 = 0.12841855; // 3 * t1 * t1
	static final double t3 = 0.008856452; // t1 * t1 * t1

	static boolean validateLab(double[] lab) {
		double l = lab[0];
		double a = lab[1];
		double b = lab[2];

		double y = (l + 16) / 116;
		double x = (Double.isNaN(a)) ? y : (y + a / 500);
		double z = (Double.isNaN(b)) ? y : (y - b / 200);

		y = Yn * labToXyz(y);
		x = Xn * labToXyz(x);
		z = Zn * labToXyz(z);

		int R = xyzToRgb(3.2404542 * x - 1.5371385 * y - 0.4985314 * z); // D65 -> sRGB
		int G = xyzToRgb(-0.9692660 * x + 1.8760108 * y + 0.0415560 * z);
		int B = xyzToRgb(0.0556434 * x - 0.2040259 * y + 1.0572252 * z);

		return R >= 0 && R <= 255 && G >= 0 && G <= 255 && B >= 0 && B <= 255;
	}

	private static int xyzToRgb(double r) {
		return (int) Math.round(255 * ((r <= 0.00304) ? (12.92 * r) : (1.055 * Math.pow(r, 1 / 2.4) - 0.055)));
	}

	private static double labToXyz(double t) {
		return (t > t1) ? (t * t * t) : (t2 * (t - t0));
	}

	public static void main(String[] args) {
		// Example Usage
		double[] lab1 = { 50, 20, 30 };
		double[] lab2 = { 100, 0, 0 };
		double[] lab3 = { -10, 0, 0 };

		System.out.println("LAB1 valid: " + validateLab(lab1));
		System.out.println("LAB2 valid: " + validateLab(lab2));
		System.out.println("LAB3 valid: " + validateLab(lab3));
	}
}