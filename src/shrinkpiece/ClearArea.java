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

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import shrinkpiece.GuideRectangle.GuideOrientation;

/**
 *
 * @author P.C.T. Kolkman
 */
public class ClearArea extends ResizableArea {
    
    private Rectangle clArea, intClearArea;
    private Bounds offsetClip;
    private Group myGuideRects;
    private double wVerGuide, hHorGuide, sTXCA, sTYCA, mXS, mYS;
    
    public ClearArea(){
        setHorizontalRestriction(true);
        
        clArea = getAreaBounds();        
        clArea.setFill(Color.RED);
        clArea.setOpacity(0.3);
        clArea.setStroke(Color.RED);
        clArea.setStrokeWidth(2.0);
        clArea.setStrokeType(StrokeType.OUTSIDE);   
         
        setGuides();
    }     
    
    @Override
    public void setOnMouseEnteredArea(){        
        clArea.setOpacity(0.6);
    }
    
    @Override
    public void setOnMouseExitedArea(){        
        clArea.setOpacity(0.3);
    }
    
    @Override
    public void setGuides(){
        getGuideGroup().getChildren().addAll(new GuideRectangle(clArea,GuideOrientation.TOP),
                new GuideRectangle(clArea,GuideOrientation.BOTTOM));
    }
                    
}
