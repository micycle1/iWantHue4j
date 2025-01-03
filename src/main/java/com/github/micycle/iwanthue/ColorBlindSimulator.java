package com.github.micycle.iwanthue;

class ColorBlindSimulator {

	enum ConfusionType {
		PROTANOPIA(0.7465, 0.2535, 1.27345, -0.07389), DEUTERANOPIA(1.4, -0.4, 0.96843, 0.00333),
		TRITANOPIA(0.1748, 0.0, 1.07678, -0.02274);
		// Add other types as needed

		private final ConfusionLine confusionLine;

		ConfusionType(double x, double y, double m, double yint) {
			this.confusionLine = new ConfusionLine(x, y, m, yint);
		}

		public ConfusionLine getConfusionLine() {
			return confusionLine;
		}
	}

	private record ConfusionLine(double x, double y, double m, double yint) {
	}

//	private final Map<String, double[]> simulateCache = new HashMap<>();
	
	public static double[] simulate(double[] lab, ConfusionType type) {
		return simulate(lab, type, 1);
	}

	public static double[] simulate(double[] lab, ConfusionType type, double amount) {
		// Cache key
//        String key = Arrays.toString(lab) + "-" + type + "-" + amount;
//        if (simulateCache.containsKey(key)) {
//            return simulateCache.get(key);
//        }

		// Get data from type
		ConfusionLine confusionLine = type.getConfusionLine();

		double confuseX = confusionLine.x;
		double confuseY = confusionLine.y;
		double confuseM = confusionLine.m;
		double confuseYint = confusionLine.yint;

		// Convert LAB to RGB
		double[] rgb = Conversion.fromLabToRgb(lab); // Assuming fromRGB() exists and correctly implemented
		double sr = rgb[0];
		double sg = rgb[1];
		double sb = rgb[2];
		double dr = sr; // destination color
		double dg = sg;
		double db = sb;

		// Convert source color into XYZ color space
		double powR = Math.pow(sr / 255.0, 2.2);
		double powG = Math.pow(sg / 255.0, 2.2);
		double powB = Math.pow(sb / 255.0, 2.2);
		double X = powR * 0.412424 + powG * 0.357579 + powB * 0.180464; // RGB->XYZ (sRGB:D65)
		double Y = powR * 0.212656 + powG * 0.715158 + powB * 0.0721856;
		double Z = powR * 0.0193324 + powG * 0.119193 + powB * 0.950444;

		// Convert XYZ into xyY Chromacity Coordinates (xy) and Luminance (Y)
		double chromaX = X / (X + Y + Z);
		double chromaY = Y / (X + Y + Z);
		if (Double.isNaN(chromaX))
			chromaX = 0.0;
		if (Double.isNaN(chromaY))
			chromaY = 0.0;

		// Generate the "Confusion Line" between the source color and the Confusion
		// Point
		double m = (chromaY - confuseY) / (chromaX - confuseX); // slope of Confusion Line
		if (Double.isNaN(m))
			m = 0.0;

		double yint = chromaY - chromaX * m; // y-intercept of confusion line (x-intercept = 0.0)

		// How far the xy coords deviate from the simulation
		double deviateX = (confuseYint - yint) / (m - confuseM);
		double deviateY = (m * deviateX) + yint;
		if (Double.isNaN(deviateX))
			deviateX = 0.0;
		if (Double.isNaN(deviateY))
			deviateY = 0.0;

		// Compute the simulated color's XYZ coords
		X = deviateX * Y / deviateY;
		Z = (1.0 - (deviateX + deviateY)) * Y / deviateY;

		// Neutral grey calculated from luminance (in D65)
		double neutralX = 0.312713 * Y / 0.329016;
		double neutralZ = 0.358271 * Y / 0.329016;

		// Difference between simulated color and neutral grey
		double diffX = neutralX - X;
		double diffZ = neutralZ - Z;
		double diffR = diffX * 3.24071 + diffZ * -0.498571; // XYZ->RGB (sRGB:D65)
		double diffG = diffX * -0.969258 + diffZ * 0.0415557;
		double diffB = diffX * 0.0556352 + diffZ * 1.05707;

		// Convert to RGB color space
		dr = X * 3.24071 + Y * -1.53726 + Z * -0.498571; // XYZ->RGB (sRGB:D65)
		dg = X * -0.969258 + Y * 1.87599 + Z * 0.0415557;
		db = X * 0.0556352 + Y * -0.203996 + Z * 1.05707;

		// Compensate simulated color towards a neutral fit in RGB space
		double fitR = ((dr < 0.0 ? 0.0 : 1.0) - dr) / diffR;
		double fitG = ((dg < 0.0 ? 0.0 : 1.0) - dg) / diffG;
		double fitB = ((db < 0.0 ? 0.0 : 1.0) - db) / diffB;
		double adjust = max( // highest value
				(fitR > 1.0 || fitR < 0.0) ? 0.0 : fitR, (fitG > 1.0 || fitG < 0.0) ? 0.0 : fitG, (fitB > 1.0 || fitB < 0.0) ? 0.0 : fitB);

		// Shift proportional to the greatest shift
		dr = dr + (adjust * diffR);
		dg = dg + (adjust * diffG);
		db = db + (adjust * diffB);

		// Apply gamma correction
		dr = Math.pow(Math.max(dr, 0.0), 1.0 / 2.2);
		dg = Math.pow(Math.max(dg, 0.0), 1.0 / 2.2);
		db = Math.pow(Math.max(db, 0.0), 1.0 / 2.2);

		// Anomylize colors
		dr = sr / 255.0 * (1.0 - amount) + dr * amount;
		dg = sg / 255.0 * (1.0 - amount) + dg * amount;
		db = sb / 255.0 * (1.0 - amount) + db * amount;

		// Convert RGB to LAB
		double[] result = Conversion.fromRgbToLab(new double[] { dr * 255.0, dg * 255.0, db * 255.0 });

//        simulateCache.put(key, result);

		return result;
	}

	private static double max(double x1, double x2, double x3) {
		return Math.max(x1, Math.max(x2, x3));
	}

}
