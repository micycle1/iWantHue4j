package com.github.micycle1.iwanthue;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.RepeatedTest;

import com.github.micycle1.iwanthue.ColorDistance.DistanceType;

class ColorDistanceTests {

	private static final int NUM_TESTS = 100;

	@RepeatedTest(NUM_TESTS)
	void testEuclideanWithRandomValues() {
		double[] labColor1 = generateRandomLabColor();
		double[] labColor2 = generateRandomLabColor();

		double euclidianDist = ColorDistance.getColorDistance(labColor1, labColor2, DistanceType.EUCLIDEAN);

		assertEquals(euclidianDist, calculateDistance(labColor1, labColor2), 1e-6);
	}

	private static double calculateDistance(double[] point1, double[] point2) {
		double dx = point1[0] - point2[0];
		double dy = point1[1] - point2[1];
		double dz = point1[2] - point2[2];

		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private static double[] generateRandomLabColor() {
		return new double[] { ThreadLocalRandom.current().nextDouble(100), ThreadLocalRandom.current().nextDouble(-128, 128),
				ThreadLocalRandom.current().nextDouble(-128, 128) };
	}

}
