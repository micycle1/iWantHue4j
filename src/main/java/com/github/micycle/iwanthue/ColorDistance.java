package com.github.micycle.iwanthue;

import java.util.Arrays;

import static com.github.micycle.iwanthue.ColorBlindSimulator.ConfusionType;

public class ColorDistance {

	enum DistanceType {
		DEFAULT, EUCLIDIAN, CMC, COMPROMISE;
	}

	public static double getColorDistance(double[] lab1, double[] lab2, DistanceType type) {
		if (type == null) {
			type = DistanceType.DEFAULT;
		}

		switch (type) {
			case DEFAULT :
			case EUCLIDIAN :
				return euclidianDistance(lab1, lab2);
			case CMC :
				return cmcDistance(lab1, lab2, 2, 1);
			case COMPROMISE :
				return compromiseDistance(lab1, lab2);
		}
		return 0;
	}

	public static double getColorDistance(double[] lab1, double[] lab2, ConfusionType type) {
		return distanceColorblind(lab1, lab2, type);

	}

	private static double distanceColorblind(double[] lab1, double[] lab2, ConfusionType type) {
		double[] lab1Cb = ColorBlindSimulator.simulate(lab1, type);
		double[] lab2Cb = ColorBlindSimulator.simulate(lab2, type);
		return cmcDistance(lab1Cb, lab2Cb, 2, 1);
	}

	private static double compromiseDistance(double[] lab1, double[] lab2) {
		double[] distances = new double[4]; // CMC + 3 colorblind types
		double[] coeffs = new double[4];
		distances[0] = cmcDistance(lab1, lab2, 2, 1);
		coeffs[0] = 1000;

		ConfusionType[] types = ConfusionType.values();
		
		for (int i = 0; i < types.length; i++) {
			double[] lab1Cb = ColorBlindSimulator.simulate(lab1, types[i]);
			double[] lab2Cb = ColorBlindSimulator.simulate(lab2, types[i]);

			if (!(Arrays.stream(lab1Cb).anyMatch(Double::isNaN) || Arrays.stream(lab2Cb).anyMatch(Double::isNaN))) {
				double c;
				switch (types[i]) {
					case PROTANOPIA :
						c = 100;
						break;
					case DEUTERANOPIA :
						c = 500;
						break;
					case TRITANOPIA :
						c = 1;
						break;
					default :
						c = 0;
				}
				distances[i + 1] = cmcDistance(lab1Cb, lab2Cb, 2, 1);
				coeffs[i + 1] = c;
			} else {
				// If simulation failed (returned NaN), set distance and coeff to 0
				// so they don't contribute to the average
				distances[i + 1] = 0;
				coeffs[i + 1] = 0;
			}
		}

		double total = 0;
		double count = 0;
		for (int i = 0; i < distances.length; i++) {
			total += coeffs[i] * distances[i];
			count += coeffs[i];
		}

		return count == 0 ? 0 : total / count;
	}

	private static double euclidianDistance(double[] lab1, double[] lab2) {
		return Math.sqrt(Math.pow(lab1[0] - lab2[0], 2) + Math.pow(lab1[1] - lab2[1], 2) + Math.pow(lab1[2] - lab2[2], 2));
	}

	private static double cmcDistance(double[] lab1, double[] lab2, double l, double c) {
		double L1 = lab1[0];
		double L2 = lab2[0];
		double a1 = lab1[1];
		double a2 = lab2[1];
		double b1 = lab1[2];
		double b2 = lab2[2];

		double C1 = Math.sqrt(Math.pow(a1, 2) + Math.pow(b1, 2));
		double C2 = Math.sqrt(Math.pow(a2, 2) + Math.pow(b2, 2));
		double deltaC = C1 - C2;
		double deltaL = L1 - L2;
		double deltaa = a1 - a2;
		double deltab = b1 - b2;
		double deltaH = Math.sqrt(Math.pow(deltaa, 2) + Math.pow(deltab, 2) - Math.pow(deltaC, 2));

		double H1 = Math.atan2(b1, a1) * (180 / Math.PI);
		while (H1 < 0) {
			H1 += 360;
		}

		double F = Math.sqrt(Math.pow(C1, 4) / (Math.pow(C1, 4) + 1900));
		double T = (164 <= H1 && H1 <= 345) ? (0.56 + Math.abs(0.2 * Math.cos(Math.toRadians(H1 + 168))))
				: (0.36 + Math.abs(0.4 * Math.cos(Math.toRadians(H1 + 35))));
		double S_L = (L1 < 16) ? 0.511 : (0.040975 * L1 / (1 + 0.01765 * L1));
		double S_C = (0.0638 * C1 / (1 + 0.0131 * C1)) + 0.638;
		double S_H = S_C * (F * T + 1 - F);

		return Math.sqrt(Math.pow(deltaL / (l * S_L), 2) + Math.pow(deltaC / (c * S_C), 2) + Math.pow(deltaH / S_H, 2));
	}

	public static void main(String[] args) {
		// Example Usage:
		double[] labColor1 = { 50.0, 25.0, 25.0 };
		double[] labColor2 = { 52.0, 23.0, 26.0 };

		double euclidianDist = getColorDistance(labColor1, labColor2, DistanceType.EUCLIDIAN);
		double cmcDist = getColorDistance(labColor1, labColor2, DistanceType.CMC);
		double compromiseDist = getColorDistance(labColor1, labColor2, DistanceType.COMPROMISE);

		System.out.println("Euclidean Distance: " + euclidianDist);
		System.out.println("CMC Distance: " + cmcDist);
		System.out.println("Compromise Distance: " + compromiseDist);

		double protanopeDist = getColorDistance(labColor1, labColor2, ConfusionType.PROTANOPIA);
		System.out.println("Protanope Distance: " + protanopeDist);
	}

}