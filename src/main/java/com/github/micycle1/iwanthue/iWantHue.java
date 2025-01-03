package com.github.micycle1.iwanthue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.github.micycle1.iwanthue.ColorDistance.DistanceType;

/**
 * @author Michael Carleton
 */
public class iWantHue {

	// https://medialab.github.io/iwanthue/js/libs/chroma.palette-gen.js

	public static void main(String[] args) throws IOException {
		List<double[]> arrays = generate(11, null, false, 50, false, DistanceType.CMC);
		List<double[]> rgbColors = arrays.stream().map(Conversion::labToRgb).toList(); // Use toList() in Java 17

		int w = 25;
		int width = w * rgbColors.size();
		int height = 100;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		var g2d = image.createGraphics(); // Use var for type inference in Java 17

		int x = 0;
		for (double[] rgb : rgbColors) {
			g2d.setColor(new java.awt.Color((int) rgb[0], (int) rgb[1], (int) rgb[2]));
			g2d.fillRect(x, 0, w, height);
			x += w;
		}
		g2d.dispose();

		ImageIO.write(image, "png", new File("palette.png"));
		System.out.println("done");
	}

	/**
	 * 
	 * @param colorsCount number of colors in the generated palette.
	 * @param checkColorIn   Function used to filter suitable colors. Takes a [r, g,
	 *                       b] color and the same [l, a, b] color as arguments.
	 * @param forceMode      Clustering method to use. Can either be k-means or
	 *                       force-vector.
	 * @param quality        Quality of the clustering: iterations factor for
	 *                       force-vector, colorspace sampling for k-means.
	 * @param ultraPrecision Ultra precision for k-means colorspace sampling?
	 * @param distanceType   Distance function to use. Can be euclidean, cmc,
	 *                       compromise (colorblind), protanope, deuteranope or
	 *                       tritanope
	 * @return list of [L,A,B] vectors
	 */
	public static List<double[]> generate(int colorsCount, Predicate<double[]> checkColorIn, boolean forceMode, int quality,
			boolean ultraPrecision, DistanceType distanceType) {
		// Default values
		Predicate<double[]> checkColor = checkColorIn == null ? rgb -> true : checkColorIn;

		if (distanceType == null) {
			distanceType = DistanceType.DEFAULT;
		}

		System.out.println("Generate palettes for " + colorsCount + " colors using color distance \"" + distanceType + "\"");

		if (forceMode) {
			// Force Vector Mode

			List<double[]> colors = new ArrayList<>();

			// It will be necessary to check if a Lab color exists in the rgb space.
			Predicate<double[]> checkLab = (lab) -> {
				double[] rgb = Conversion.labToRgb(lab);
				return Conversion.validateLab(lab) && checkColor.test(rgb);
			};

			// Init
			for (int i = 0; i < colorsCount; i++) {
				// Find a valid Lab color
				double[] color = { 100 * Math.random(), 100 * (2 * Math.random() - 1), 100 * (2 * Math.random() - 1) };
				while (!checkLab.test(color)) {
					color = new double[] { 100 * Math.random(), 100 * (2 * Math.random() - 1), 100 * (2 * Math.random() - 1) };
				}
				colors.add(color);
			}

			// Force vector: repulsion
			double repulsion = 100;
			double speed = 100;
			int steps = quality * 20;
			while (steps-- > 0) {
				// Init
				double[][] vectors = new double[colors.size()][3];
				// Compute Force
				for (int i = 0; i < colors.size(); i++) {
					double[] colorA = colors.get(i);
					for (int j = 0; j < i; j++) {
						double[] colorB = colors.get(j);

						// repulsion force
						double dl = colorA[0] - colorB[0];
						double da = colorA[1] - colorB[1];
						double db = colorA[2] - colorB[2];
						double d = ColorDistance.getColorDistance(colorA, colorB, distanceType);
						if (d > 0) {
							double force = repulsion / Math.pow(d, 2);

							vectors[i][0] += dl * force / d;
							vectors[i][1] += da * force / d;
							vectors[i][2] += db * force / d;

							vectors[j][0] -= dl * force / d;
							vectors[j][1] -= da * force / d;
							vectors[j][2] -= db * force / d;
						} else {
							// Jitter
							vectors[j][0] += 2 - 4 * Math.random();
							vectors[j][1] += 2 - 4 * Math.random();
							vectors[j][2] += 2 - 4 * Math.random();
						}
					}
				}
				// Apply Force
				for (int i = 0; i < colors.size(); i++) {
					double[] color = colors.get(i);
					double displacement = speed
							* Math.sqrt(Math.pow(vectors[i][0], 2) + Math.pow(vectors[i][1], 2) + Math.pow(vectors[i][2], 2));
					if (displacement > 0) {
						double ratio = speed * Math.min(0.1, displacement) / displacement;
						double[] candidateLab = { color[0] + vectors[i][0] * ratio, color[1] + vectors[i][1] * ratio,
								color[2] + vectors[i][2] * ratio };
						if (checkLab.test(candidateLab)) {
							colors.set(i, candidateLab);
						}
					}
				}
			}
			return colors;

		} else {
			// K-Means Mode
			Predicate<double[]> checkColor2 = (lab) -> {
				// Check that a color is valid: it must verify our checkColor condition, but
				// also be in the color space
				double[] rgb = Conversion.labToRgb(lab);

				return Conversion.validateLab(lab) && checkColor.test(rgb);
			};

			List<double[]> kMeans = new ArrayList<>();
			for (int i = 0; i < colorsCount; i++) {
				double[] lab = { 100 * Math.random(), 100 * (2 * Math.random() - 1), 100 * (2 * Math.random() - 1) };
				int failsafe = 10;
				while (!checkColor2.test(lab) && failsafe-- > 0) {
					lab = new double[] { 100 * Math.random(), 100 * (2 * Math.random() - 1), 100 * (2 * Math.random() - 1) };
				}
				kMeans.add(lab);
			}

			List<double[]> colorSamples = new ArrayList<>();
			List<Integer> samplesClosest = new ArrayList<>();
			if (ultraPrecision) {
				for (int l = 0; l <= 100; l += 1) {
					for (int a = -100; a <= 100; a += 5) {
						for (int b = -100; b <= 100; b += 5) {
							if (checkColor2.test(new double[] { l, a, b })) {
								colorSamples.add(new double[] { l, a, b });
								samplesClosest.add(null);
							}
						}
					}
				}
			} else {
				for (int l = 0; l <= 100; l += 5) {
					for (int a = -100; a <= 100; a += 10) {
						for (int b = -100; b <= 100; b += 10) {
							if (checkColor2.test(new double[] { l, a, b })) {
								colorSamples.add(new double[] { l, a, b });
								samplesClosest.add(null);
							}
						}
					}
				}
			}

			// Steps
			int steps = quality;
			while (steps-- > 0) {
				// kMeans -> Samples Closest
				for (int i = 0; i < colorSamples.size(); i++) {
					double[] lab = colorSamples.get(i);
					double minDistance = Double.POSITIVE_INFINITY;
					for (int j = 0; j < kMeans.size(); j++) {
						double[] kMean = kMeans.get(j);
						double distance = ColorDistance.getColorDistance(lab, kMean, distanceType);
						if (distance < minDistance) {
							minDistance = distance;
							samplesClosest.set(i, j);
						}
					}
				}

				// Samples -> kMeans
				List<double[]> freeColorSamples = new ArrayList<>(colorSamples);
				for (int j = 0; j < kMeans.size(); j++) {
					int count = 0;
					double[] candidateKMean = { 0, 0, 0 };
					for (int i = 0; i < colorSamples.size(); i++) {
						if (samplesClosest.get(i) == j) {
							count++;
							candidateKMean[0] += colorSamples.get(i)[0];
							candidateKMean[1] += colorSamples.get(i)[1];
							candidateKMean[2] += colorSamples.get(i)[2];
						}
					}
					if (count != 0) {
						candidateKMean[0] /= count;
						candidateKMean[1] /= count;
						candidateKMean[2] /= count;
					}

					if (count != 0 && checkColor2.test(new double[] { candidateKMean[0], candidateKMean[1], candidateKMean[2] })) {
						kMeans.set(j, candidateKMean);
					} else {
						// The candidate kMean is out of the boundaries of the color space, or unfound.
						if (freeColorSamples.size() > 0) {
							// We just search for the closest FREE color of the candidate kMean
							double minDistance = Double.POSITIVE_INFINITY;
							int closest = -1;
							for (int i = 0; i < freeColorSamples.size(); i++) {
								double distance = ColorDistance.getColorDistance(freeColorSamples.get(i), candidateKMean, distanceType);
								if (distance < minDistance) {
									minDistance = distance;
									closest = i;
								}
							}
							if (closest >= 0) {
								kMeans.set(j, colorSamples.get(closest));
							}

						} else {
							// Then we just search for the closest color of the candidate kMean
							double minDistance = Double.POSITIVE_INFINITY;
							int closest = -1;
							for (int i = 0; i < colorSamples.size(); i++) {
								double distance = ColorDistance.getColorDistance(colorSamples.get(i), candidateKMean, distanceType);
								if (distance < minDistance) {
									minDistance = distance;
									closest = i;
								}
							}
							if (closest >= 0) {
								kMeans.set(j, colorSamples.get(closest));
							}
						}
					}

					final int current_j = j;
					freeColorSamples.removeIf(color -> Arrays.equals(color, kMeans.get(current_j)));
				}
			}
			return kMeans;
		}
	}

}
