package com.github.micycle1.iwanthue;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

public class ConversionTests {

	@Test
	void labToRgb_validInput_returnsCorrectRgb() {
		double[] lab = { 78.25, -17.71, 76.47 };
		double[] expectedRgb = { 200, 200, 20 };
		double[] actualRgb = Conversion.labToRgb(lab);

		assertArrayEquals(expectedRgb, actualRgb, 0.02);
	}

	@Test
	void rgbToLab_validInput_returnsCorrectLab() {
		double[] rgb = { 50, 150, 250 };
		double[] expectedLab = { 61.127, 5.999, -57.839 };
		double[] actualLab = Conversion.rgbToLab(rgb);

		assertArrayEquals(expectedLab, actualLab, 0.02);
	}

	private static final int NUM_TESTS = 100;

	@RepeatedTest(NUM_TESTS)
	@DisplayName("RGB to Lab to RGB Conversion - ")
	void rgbToLabToRgb_randomColors_returnsOriginalRgb() {
		double[] rgb = generateRandomRgb();

		double[] lab = Conversion.rgbToLab(rgb);
		double[] convertedRgb = Conversion.labToRgb(lab);

		assertArrayEquals(rgb, convertedRgb, 0.01);
	}

	private double[] generateRandomRgb() {
		double[] rgb = new double[3];
		rgb[0] = ThreadLocalRandom.current().nextDouble() * 255; // R: 0-255
		rgb[1] = ThreadLocalRandom.current().nextDouble() * 255; // G: 0-255
		rgb[2] = ThreadLocalRandom.current().nextDouble() * 255; // B: 0-255
		return rgb;
	}
}