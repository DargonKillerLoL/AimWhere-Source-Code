package me.kiras.aimwhere.libraries.slick.opengl.pbuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;
import me.kiras.aimwhere.libraries.slick.Graphics;
import me.kiras.aimwhere.libraries.slick.Image;
import me.kiras.aimwhere.libraries.slick.SlickException;
import me.kiras.aimwhere.libraries.slick.opengl.SlickCallable;
import me.kiras.aimwhere.libraries.slick.opengl.Texture;
import me.kiras.aimwhere.libraries.slick.opengl.TextureImpl;
import me.kiras.aimwhere.libraries.slick.opengl.InternalTextureLoader;
import me.kiras.aimwhere.libraries.slick.util.Log;

/**
 * A graphics implementation that renders to a PBuffer
 *
 * @author kevin
 */
public class PBufferGraphics extends Graphics {

	/** The pbuffer we're going to render to */
	private Pbuffer pbuffer;
	/** The image we're we're sort of rendering to */
	private Image image;
	
	/**
	 * Create a new graphics context around a pbuffer
	 * 
	 * @param image The image we're rendering to
	 * @throws SlickException Indicates a failure to use pbuffers
	 */
	public PBufferGraphics(Image image) throws SlickException {
		super(image.getTexture().getTextureWidth(), image.getTexture().getTextureHeight());
		this.image = image;
		
		Log.debug("Creating pbuffer(rtt) "+image.getWidth()+"x"+image.getHeight());
		if ((Pbuffer.getCapabilities() & Pbuffer.PBUFFER_SUPPORTED) == 0) {
			throw new SlickException("Your OpenGL card does not support PBuffers and hence can't handle the dynamic images required for this application.");
		}
		if ((Pbuffer.getCapabilities() & Pbuffer.RENDER_TEXTURE_SUPPORTED) == 0) {
			throw new SlickException("Your OpenGL card does not support Render-To-Texture and hence can't handle the dynamic images required for this application.");
		}
	
		init();
	}	

	/**
	 * Initialise the PBuffer that will be used to render to
	 * 
	 * @throws SlickException
	 */
	private void init() throws SlickException {
		try {
			Texture tex = InternalTextureLoader.get().createTexture(image.getWidth(), image.getHeight(), image.getFilter());
			
			final RenderTexture rt = new RenderTexture(false, true, false, false, RenderTexture.RENDER_TEXTURE_2D, 0);
			pbuffer = new Pbuffer(screenWidth, screenHeight, new PixelFormat(8, 0, 0), rt, null);

			// Initialise state of the pbuffer context.
			pbuffer.makeCurrent();

			initGL();
			GL.glBindTexture(GL11.GL_TEXTURE_2D, tex.getTextureID());
			pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
			image.draw(0,0);
			image.setTexture(tex);
			
			Display.makeCurrent();
		} catch (Exception e) {
			Log.error(e);
			throw new SlickException("Failed to create PBuffer for dynamic image. OpenGL driver failure?");
		}
	}

	/**
	 * @see me.kiras.aimwhere.libraries.slick.Graphics#disable()
	 */
	protected void disable() {
		GL.flush();
		
		// Bind the texture after rendering.
		GL.glBindTexture(GL11.GL_TEXTURE_2D, image.getTexture().getTextureID());
		pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
		
		try {
			Display.makeCurrent();
		} catch (LWJGLException e) {
			Log.error(e);
		}
		
		SlickCallable.leaveSafeBlock();
	}

	/**
	 * @see me.kiras.aimwhere.libraries.slick.Graphics#enable()
	 */
	protected void enable() {
		SlickCallable.enterSafeBlock();
		
		try {
			if (pbuffer.isBufferLost()) {
				pbuffer.destroy();
				init();
			}

			pbuffer.makeCurrent();
		} catch (Exception e) {
			Log.error("Failed to recreate the PBuffer");
			throw new RuntimeException(e);
		}
		
		// Put the renderer contents to the texture
		GL.glBindTexture(GL11.GL_TEXTURE_2D, image.getTexture().getTextureID());
		pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
		TextureImpl.unbind();
		initGL();
	}
	
	/**
	 * Initialise the GL context
	 */
	protected void initGL() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glShadeModel(GL11.GL_SMOOTH);        
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);                    
        
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);                
        GL11.glClearDepth(1);                                       
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glViewport(0,0,screenWidth,screenHeight);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		
		enterOrtho();
	}
	
	/**
	 * Enter the orthographic mode 
	 */
	protected void enterOrtho() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, screenWidth, 0, screenHeight, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
	}
	
	/**
	 * @see me.kiras.aimwhere.libraries.slick.Graphics#destroy()
	 */
	public void destroy() {
		super.destroy();
		
		pbuffer.destroy();
	}
	
	/**
	 * @see me.kiras.aimwhere.libraries.slick.Graphics#flush()
	 */
	public void flush() {
		super.flush();
		
		image.flushPixelData();
	}
}
