/*
 * Copyright (C) 2020 P.C.T. Kolkman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package shrinkpiece;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import shrinkpiece.GuideRectangle.GuideOrientation;

/**
 *
 * @author P.C.T. Kolkman
 */
public class TwoDimensionalAdjustment extends ResizableArea {

    private Rectangle adjustRect;
    
    public TwoDimensionalAdjustment(){        
        adjustRect = getAreaBounds();
        adjustRect.setFill(Color.TRANSPARENT);
        adjustRect.setStroke(Color.GREEN);
        adjustRect.setStrokeWidth(0.5);
        
        setGuides();     
    }        

    @Override
    public void setOnMouseEnteredArea() {
    }
    @Override
    public void setOnMouseExitedArea() {
    }
    @Override
    public void setGuides() {
        getGuideGroup().getChildren().addAll(
            new GuideRectangle(adjustRect,GuideOrientation.TOP),
            new GuideRectangle(adjustRect,GuideOrientation.RIGHT),
            new GuideRectangle(adjustRect,GuideOrientation.BOTTOM),
            new GuideRectangle(adjustRect,GuideOrientation.LEFT)
        );    
    }
    
}
