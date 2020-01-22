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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 *
 * @author P.C.T. Kolkman
 */
public class SizingArrow extends Group {
    
    private Line arrShunt, leftPoint, rightPoint;
    private Circle rDot;
    private Color arrColor;
    private final double arrWidth = 1.0, dotRad = 2.0; 
    
    public SizingArrow(){
        arrColor = Color.NAVY;
        
        arrShunt = new Line();
        arrShunt.setStroke(arrColor);
        leftPoint = new Line();
        leftPoint.setStroke(arrColor);
        rightPoint = new Line();
        rightPoint.setStroke(arrColor);
        rDot = new Circle(dotRad);
        rDot.setFill(arrColor);
        rDot.setStroke(Color.TRANSPARENT);
        rDot.setVisible(false);
        
        getChildren().addAll(arrShunt, leftPoint, rightPoint, rDot);    
        
        setOnMouseEntered((mE)->{
            arrShunt.setStrokeWidth(arrWidth + 1.0);
            leftPoint.setStrokeWidth(arrWidth + 1.0);
            rightPoint.setStrokeWidth(arrWidth + 1.0);
            rDot.setRadius(dotRad + 1.0);            
        });
        setOnMouseExited((mEx)->{
            arrShunt.setStrokeWidth(arrWidth);
            leftPoint.setStrokeWidth(arrWidth);
            rightPoint.setStrokeWidth(arrWidth);
            rDot.setRadius(dotRad);            
        });
    }
    
    public void resize(Bounds to){        
        arrShunt.setStartX(to.getMinX());
        arrShunt.setStartY(to.getMinY());
        arrShunt.setEndX(to.getMaxX());
        arrShunt.setEndY(to.getMaxY());
        leftPoint.setStartX(to.getMaxX());
        leftPoint.setStartY(to.getMaxY());
        leftPoint.setEndX(to.getMaxX());
        leftPoint.setEndY(to.getMaxY() - 10.0);
        rightPoint.setStartX(to.getMaxX());
        rightPoint.setStartY(to.getMaxY());
        rightPoint.setEndX(to.getMaxX() - 10.0);
        rightPoint.setEndY(to.getMaxY());
        rDot.setTranslateX(to.getMaxX());
        rDot.setTranslateY(to.getMaxY());
    }
    
}
