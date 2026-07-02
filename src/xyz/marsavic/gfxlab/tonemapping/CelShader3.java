package xyz.marsavic.gfxlab.tonemapping;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Animation;
import xyz.marsavic.gfxlab.Array2;
import xyz.marsavic.gfxlab.Color;
import xyz.marsavic.gfxlab.Matrix;
import xyz.marsavic.gfxlab.UtilsGL;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.resources.Rr;
import xyz.marsavic.utils.Numeric;


public record CelShader3(
		F1<Rr<Matrix<Color>>, Integer> frColor,
		F1<Rr<Matrix<Color>>, Integer> frNormal,
		F1<Rr<Matrix<Color>>, Integer> frDepth,
		F1<ColorTransform, Array2<Color>> fColorTransform,
		int supersample,
		int lightnessBands,
		int chromaBands,
		int hueBands,
		double edgeThreshold,
		double depthEdgeScale
) implements Animation {

	public CelShader3 {
		if (supersample < 1) throw new IllegalArgumentException("supersample must be at least 1.");
		if (lightnessBands < 1 || chromaBands < 1 || hueBands < 1)
			throw new IllegalArgumentException("lightnessBands, chromaBands and hueBands must be at least 1.");
	}


	private static final double OKHCL_CHROMA_NORM = 0.4;
	private static final double ACHROMATIC_EPS = 0.002;

	private static final double[][] SOBEL_X = {
			{-1, 0, 1},
			{-2, 0, 2},
			{-1, 0, 1},
	};
	private static final double[][] SOBEL_Y = {
			{-1, -2, -1},
			{ 0,  0,  0},
			{ 1,  2,  1},
	};

	@Override
	public Rr<Matrix<Integer>> at(Integer iFrame) {
		Rr<Matrix<Color>> rColor  = frColor.at(iFrame);
		Rr<Matrix<Color>> rNormal = frNormal.at(iFrame);
		Rr<Matrix<Color>> rDepth  = frDepth.at(iFrame);

		Rr<Matrix<Integer>> rOut = rColor.f(mColor -> rNormal.f(mNormal -> rDepth.f(mDepth -> shade(mColor, mNormal, mDepth))));

		rColor.release();
		rNormal.release();
		rDepth.release();
		return rOut;
	}

	private Rr<Matrix<Integer>> shade(Matrix<Color> mColor, Matrix<Color> mNormal, Matrix<Color> mDepth) {
		int s = supersample;
		Vector sizeHi = mColor.size();
		int w = sizeHi.xInt() / s;
		int h = sizeHi.yInt() / s;
		Vector sizeOut = Vector.xy(w, h);

		ColorTransform tone = fColorTransform.at(mColor);

		Rr<Matrix<Integer>> rOut = UtilsGL.matricesInt.obtain(sizeOut, true);
		final double invSamples = 1.0 / (s * s);
		// po izlaznom pikselu usrednjava s*s hi-res uzoraka (supersampling) za anti-aliasing
		rOut.a(mOut -> mOut.fill((x, y) -> {
			Color sum = Color.BLACK;
			int xHi = x * s, yHi = y * s;
			for (int dy = 0; dy < s; dy++) {
				for (int dx = 0; dx < s; dx++) {
					sum = sum.add(shadeSample(mColor, mNormal, mDepth, tone, xHi + dx, yHi + dy, s));
				}
			}
			return sum.mul(invSamples).code();
		}));
		return rOut;
	}

	private Color shadeSample(Matrix<Color> mColor, Matrix<Color> mNormal, Matrix<Color> mDepth, ColorTransform tone, int x, int y, int step) {
		Color banded = quantize(tone.at(mColor.at(x, y)));

		// depthEdgeScale pojačava depth ivice (posle /farPlane su male), pa siluete dobijaju prioritet nad pregibima
		double e = Math.max(normalEdge(mNormal, x, y, step), depthEdge(mDepth, x, y, step) * depthEdgeScale);
		// smoothstep omekšava ivicu da ne bude nazubljena
		double outlineWeight = smoothstep(0.5 * edgeThreshold, 1.5 * edgeThreshold, e);

		// na punoj ivici boja ide u crno, na delimičnoj samo potamni
		return banded.mul(1.0 - outlineWeight);
	}

	private Color quantize(Color color) {
		Vec3 hcl = color.okhcl();
		double ql = bandify(hcl.z(), lightnessBands);
		// ispod ACHROMATIC_EPS je hue samo šum (atan2 od argumenata blizu nule), pa se kvantizuje samo lightness i piksel ostaje siv
		if (hcl.y() < ACHROMATIC_EPS)
			return Color.okhcl(0, 0, ql).clampTo01();
		double qh = bandify(hcl.x(), hueBands);
		// chroma nema prirodnu gornju granicu (za razliku od L i hue), pa se skalira u [0,1] preko OKHCL_CHROMA_NORM
		double qc = bandify(hcl.y() / OKHCL_CHROMA_NORM, chromaBands) * OKHCL_CHROMA_NORM;
		return Color.okhcl(qh, qc, ql).clampTo01();
	}

	private static double bandify(double v, int n) {
		// zaokrugli v na centar trake; ceo opseg trake pada na istu vrednost, odatle ravne cel površine
		int band = (int) Math.floor(v * n);
		if (band < 0) band = 0;
		if (band >= n) band = n - 1;
		return (band + 0.5) / n;
	}

	private static double smoothstep(double edge0, double edge1, double x) {
		if (edge1 <= edge0) return x < edge0 ? 0.0 : 1.0;
		double t = Numeric.clamp((x - edge0) / (edge1 - edge0));
		return t * t * (3.0 - 2.0 * t);
	}


	private static double normalEdge(Matrix<Color> mSrc, int x, int y, int step) {
		// gradijent normale, hvata pregibe unutar objekta gde se površina savija
		double gr = edgeStrength(mSrc, x, y, 0, step);
		double gg = edgeStrength(mSrc, x, y, 1, step);
		double gb = edgeStrength(mSrc, x, y, 2, step);
		return Math.sqrt(gr * gr + gg * gg + gb * gb);
	}

	private static double depthEdge(Matrix<Color> mSrc, int x, int y, int step) {
		// gradijent dubine, hvata siluete i mesta gde jedan objekat zaklanja drugi
		return edgeStrength(mSrc, x, y, 0, step);
	}

	private static double edgeStrength(Matrix<Color> mSrc, int x, int y, int channel, int step) {
		double gx = convolve(mSrc, x, y, channel, step, SOBEL_X);
		double gy = convolve(mSrc, x, y, channel, step, SOBEL_Y);
		return Math.sqrt(gx * gx + gy * gy);
	}

	private static double convolve(Matrix<Color> mSrc, int x, int y, int channel, int step, double[][] k) {
		int rows = k.length, cols = k[0].length;
		int cy = rows / 2, cx = cols / 2;
		double acc = 0;
		for (int j = 0; j < rows; j++) {
			for (int i = 0; i < cols; i++) {
				acc += k[j][i] * sample(mSrc, x + (i - cx) * step, y + (j - cy) * step, channel);
			}
		}
		return acc;
	}

	private static double sample(Matrix<Color> mSrc, int x, int y, int channel) {
		Vector size = mSrc.size();
		int clampedX = Math.clamp(x, 0, size.xInt() - 1);
		int clampedY = Math.clamp(y, 0, size.yInt() - 1);
		Vec3 rgb = mSrc.at(clampedX, clampedY).rgb();
		return switch (channel) {
			case 0 -> rgb.x();
			case 1 -> rgb.y();
			default -> rgb.z();
		};
	}

}
