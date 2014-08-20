/*
 * 2w2m
 */

package org.mapfish.print.config.layout;

import java.util.List;

import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;

public class AbsoluteColumnsBlock extends Block {
    private List<Block> items;
    private int[] widths = null;
    /*
    private int absoluteX = Integer.MIN_VALUE;
    private int absoluteY = Integer.MIN_VALUE;
    private int width = Integer.MIN_VALUE;
    */
    private String absoluteX = null;
//    private double absoluteX = Double.MIN_VALUE;
    private String absoluteY = null;
    private String widthVar = null;
    private int nbColumns = Integer.MIN_VALUE;
    private TableConfig config = null;

    public void render(final PJsonObject params, PdfElement target, final RenderingContext context) throws DocumentException {

        if (isAbsolute()) {
            context.getCustomBlocks().addAbsoluteDrawer(new PDFCustomBlocks.AbsoluteDrawer() {
                public void render(PdfContentByte dc) throws DocumentException {
                    final PdfPTable table = PDFUtils.buildTable(items, params, context, nbColumns, config);
                    if (table != null) {
                    	final float width = getWidth(context, params);
                        table.setTotalWidth(width);
                        table.setLockedWidth(true);

                        if (widths != null) {
                            table.setWidths(widths);
                        }
                        final float absX = getAbsoluteX(context, params);
                        final float absY = getAbsoluteY(context, params);
                        table.writeSelectedRows(0, -1, absX, absY, dc);
                    }
                }
            });
        } else {
            final PdfPTable table = PDFUtils.buildTable(items, params, context, nbColumns, config);
            if (table != null) {
                if (widths != null) {
                    table.setWidths(widths);
                }

                table.setSpacingAfter((float) spacingAfter);
                target.add(table);
            }
        }
    }

    public void setItems(List<Block> items) {
        this.items = items;
    }

    public void setWidths(int[] widths) {
        this.widths = widths;
    }

    public void setAbsoluteX(String absoluteX) {
        this.absoluteX = absoluteX;
    }

    public float getAbsoluteX(RenderingContext context, PJsonObject params) {
      return Float.parseFloat(PDFUtils.evalString(context, params, absoluteX));
    }
    
    public void setAbsoluteY(String absoluteY) {
        this.absoluteY = absoluteY;
    }

    public float getAbsoluteY(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, absoluteY));
      }
      
    public void setWidth(String width) {
        this.widthVar = width;
    }

    public float getWidth(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, widthVar));
      }
      
    public void setNbColumns(int nbColumns) {
        this.nbColumns = nbColumns;
    }

    public boolean isAbsolute() {
        return absoluteX != null &&
                absoluteY != null &&
                widthVar != null;
    }

    public MapBlock getMap() {
        for (Block item : items) {
            MapBlock result = item.getMap();
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void setConfig(TableConfig config) {
        this.config = config;
    }

    public void validate() {
        super.validate();
        if (items == null) throw new InvalidValueException("items", "null");
        if (items.size() < 1) throw new InvalidValueException("items", "[]");

        if (!((absoluteX != null && absoluteY != null && widthVar != null) ||
                (absoluteX == null && absoluteY == null && widthVar == null))) {
            throw new InvalidValueException("absoluteX, absoluteY or width", "all of them must be defined or none");
        }

        for (int i = 0; i < items.size(); i++) {
            final Block item = items.get(i);
            item.validate();
            if (item.isAbsolute()) {
                throw new InvalidValueException("items", "Cannot put an absolute block in a !columns or !table block");
            }
        }

        if (config != null) config.validate();
    }
}
