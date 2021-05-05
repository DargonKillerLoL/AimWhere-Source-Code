
package me.kiras.aimwhere.libraries.slick.font.effects;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import me.kiras.aimwhere.libraries.slick.UnicodeFont;
import me.kiras.aimwhere.libraries.slick.font.Glyph;

/**
 * Applys a {@link BufferedImageOp} filter to glyphs. Many filters can be found 
 * here: http://www.jhlabs.com/ip/filters/index.html
 * 
 * @author Nathan Sweet <misc@n4te.com>
 */
public class FilterEffect implements Effect {
	/** The filter to be applied */
	private BufferedImageOp filter;

	/**
	 * Default constructor for injection
	 */
	public FilterEffect () {
	}

	/**
	 * Create a new filtering effect based on a convolution operation
	 * 
	 * @param filter The filter to apply
	 */
	public FilterEffect (BufferedImageOp filter) {
		this.filter = filter;
	}

	/**
	 * @see me.kiras.aimwhere.libraries.slick.font.effects.Effect#draw(BufferedImage, Graphics2D, me.kiras.aimwhere.libraries.slick.UnicodeFont, me.kiras.aimwhere.libraries.slick.font.Glyph)
	 */
	public void draw(BufferedImage image, Graphics2D g, UnicodeFont unicodeFont, Glyph glyph) {
		BufferedImage scratchImage = EffectUtil.getScratchImage();
		filter.filter(image, scratchImage);
		image.getGraphics().drawImage(scratchImage, 0, 0, null);
	}

	/**
	 * Get the filter being applied by this effect
	 * 
	 * @return The filter being applied by this effect
	 */
	public BufferedImageOp getFilter() {
		return filter;
	}

	/**
	 * Set the filter being applied by this effect
	 * 
	 * @param filter The filter being used by this effect
	 */
	public void setFilter(BufferedImageOp filter) {
		this.filter = filter;
	}
}
