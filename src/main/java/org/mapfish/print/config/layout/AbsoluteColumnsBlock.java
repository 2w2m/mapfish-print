/*
 * 2w2m
 */

package org.mapfish.print.config.layout;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPTable;
import org.mapfish.print.InvalidValueException;
import org.mapfish.print.PDFCustomBlocks;
import org.mapfish.print.PDFUtils;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;

import java.util.List;

public class AbsoluteColumnsBlock extends Block {
    private List<Block> items;
    private int[] widths = null;
    /*
    private int absoluteX = Integer.MIN_VALUE;
    private int absoluteY = Integer.MIN_VALUE;
    private int width = Integer.MIN_VALUE;
    */
    private double absoluteX = Double.MIN_VALUE;
//    private double absoluteX = Double.MIN_VALUE;
    private double absoluteY = Double.MIN_VALUE;
    private double width = Double.MIN_VALUE;
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

    public void setAbsoluteX(double absoluteX) {
        this.absoluteX = absoluteX;
    }

    public float getAbsoluteX(RenderingContext context, PJsonObject params) {
      return Float.parseFloat(PDFUtils.evalString(context, params, String.valueOf(absoluteX), null));
    }
    
    public void setAbsoluteY(double absoluteY) {
        this.absoluteY = absoluteY;
    }

    public float getAbsoluteY(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, String.valueOf(absoluteY), null));
      }
      
    public void setWidth(double width) {
        this.width = width;
    }

    public float getWidth(RenderingContext context, PJsonObject params) {
        return Float.parseFloat(PDFUtils.evalString(context, params, String.valueOf(width), null));
      }
      
    public void setNbColumns(int nbColumns) {
        this.nbColumns = nbColumns;
    }

    public boolean isAbsolute() {
        return absoluteX != Double.MIN_VALUE &&
                absoluteY != Double.MIN_VALUE &&
                width != Double.MIN_VALUE;
    }

    public MapBlock getMap(String name) {
        for (Block item : items) {
            MapBlock result = item.getMap(name);
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

        if (!((absoluteX != Double.MIN_VALUE && absoluteY != Double.MIN_VALUE && width != Double.MIN_VALUE) ||
                (absoluteX == Double.MIN_VALUE && absoluteY == Double.MIN_VALUE && width == Double.MIN_VALUE))) {
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
