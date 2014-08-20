/*
 * 2w2m
 */

package org.mapfish.print.config.layout;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.json.JSONException;
import org.json.JSONObject;
import org.mapfish.print.ChunkDrawer;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;

/**
 * 
 */
public class ConfigurableImageBlock extends Block {
    private String url = null;
    private String maxWidth = null;
    private String maxHeight = null;
    private String rotation = "0";

    public void render(PJsonObject params, PdfElement target, RenderingContext context) throws DocumentException {
        final URI url;
        try {
            final String urlTxt = PDFUtils.evalString(context, params, this.url);
            url = new URI(urlTxt);
        } catch (URISyntaxException e) {
            throw new InvalidValueException("url", this.url, e);
        }
        if (url.getPath().endsWith(".svg")) {
            drawSVG(context, params, target, url);
        } else {
        	final double width = getMaxWidth(context, params);
        	final double height = getMaxHeight(context, params);
            target.add(PDFUtils.createImageChunk(context, width, height, url, getRotationRadian(context, params)));
        }
    }

    private float getRotationRadian(RenderingContext context, PJsonObject params) {
        return (float) (Float.parseFloat(PDFUtils.evalString(context, params, this.rotation)) * Math.PI / 180.0F);
    }

    private void drawSVG(RenderingContext context, PJsonObject params, PdfElement paragraph, URI url) throws DocumentException {
        final TranscoderInput ti = new TranscoderInput(url.toString());
        final PrintTranscoder pt = new PrintTranscoder();
        pt.addTranscodingHint(PrintTranscoder.KEY_SCALE_TO_PAGE, Boolean.TRUE);
        pt.transcode(ti, null);

    	final double width = getMaxWidth(context, params);
    	final double height = getMaxHeight(context, params);
        final Paper paper = new Paper();
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        final float rotation = getRotationRadian(context, params);

        final PageFormat pf = new PageFormat();
        pf.setPaper(paper);

        final SvgDrawer drawer = new SvgDrawer(context.getCustomBlocks(), rotation, pt, pf, width, height);

        //register a drawer that will do the job once the position of the map is known
        paragraph.add(PDFUtils.createPlaceholderTable(width, height, spacingAfter, drawer, align, context.getCustomBlocks()));
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    private double getMaxWidth(RenderingContext context, PJsonObject params) {
    	return Double.parseDouble(PDFUtils.evalString(context, params, this.maxWidth));
    }
    
    public void setMaxWidth(String maxWidth) {
        this.maxWidth = maxWidth;
        //if (maxWidth < 0.0) throw new InvalidValueException("maxWidth", maxWidth);
    }

    private double getMaxHeight(RenderingContext context, PJsonObject params) {
//		// Funktioniert leider nicht
//    	String param = params.optString(maxHeight, null);
//    	if (param == null) {
//    		JSONObject obj = params.getInternalObj();
//    		try {
//				obj.put(maxHeight, "200");
//			} catch (JSONException e) {
//			}
//    	}
    	return Double.parseDouble(PDFUtils.evalString(context, params, this.maxHeight));
    }
    
    public void setMaxHeight(String maxHeight) {
        this.maxHeight = maxHeight;
        //if (maxHeight < 0.0) throw new InvalidValueException("maxHeight", maxHeight);
    }

    public void setRotation(String rotation) {
        this.rotation = rotation;
    }

    private class SvgDrawer extends ChunkDrawer {
        private final float rotation;
        private final PrintTranscoder pt;
        private final PageFormat pf;
        private final double imgWidth;
        private final double imgHeight;

        public SvgDrawer(PDFCustomBlocks customBlocks, float rotation, PrintTranscoder pt, PageFormat pf, double imgWidth, double imgHeight) {
            super(customBlocks);
            this.rotation = rotation;
            this.pt = pt;
            this.pf = pf;
            this.imgWidth = imgWidth;
            this.imgHeight = imgHeight;
        }

        public void renderImpl(Rectangle rectangle, PdfContentByte dc) {
            dc.saveState();
            Graphics2D g2 = null;
            try {
                final AffineTransform t = AffineTransform.getTranslateInstance(rectangle.getLeft(), rectangle.getBottom());
                if (rotation != 0.0F) {
                    t.rotate(rotation, imgWidth / 2.0, imgHeight / 2.0);
                }
                dc.transform(t);
                g2 = dc.createGraphics((float) imgWidth, (float) imgHeight);

                //avoid a warning from Batik
                System.setProperty("org.apache.batik.warn_destination", "false");
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_TRANSCODING, RenderingHintsKeyExt.VALUE_TRANSCODING_PRINTING);
                g2.setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING, RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);

                pt.print(g2, pf, 0);
            } finally {
                if (g2 != null) {
                    g2.dispose();
                }
                dc.restoreState();
            }
        }

    }

    @Override
    public void validate() {
        super.validate();
        if (url == null) throw new InvalidValueException("url", "null");
    }
}
