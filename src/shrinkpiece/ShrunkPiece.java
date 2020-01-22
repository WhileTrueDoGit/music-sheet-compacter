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

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/**
 *
 * @author P.C.T. Kolkman
 */
public class ShrunkPiece extends Group {
            
    public static final double WSuP = 64.0, HSuP = 80.0;
    private double doubleBorderWidth;
    private WritableImage shrunkPiece;
    private ImageView sPPreview;
    private Rectangle borderSPPrev;
    private DropShadow suPieceShadow;
    
    public ShrunkPiece(WritableImage pieceToStore){
        shrunkPiece = pieceToStore;
        
        doubleBorderWidth = 2.0*4.0;
        borderSPPrev = new Rectangle(0.0,0.0,WSuP, HSuP);
        borderSPPrev.setFill(Color.TRANSPARENT);
        borderSPPrev.setStroke(Color.NAVY);
        borderSPPrev.setStrokeWidth(0.5*doubleBorderWidth);
        borderSPPrev.setStrokeType(StrokeType.INSIDE);
        
        sPPreview = new ImageView(shrunkPiece);    
        sPPreview.setTranslateX(0.5*doubleBorderWidth);
        sPPreview.setTranslateY(0.5*doubleBorderWidth);
        sPPreview.setFitWidth(WSuP - doubleBorderWidth);
        sPPreview.setFitHeight(HSuP - doubleBorderWidth);
        
        getChildren().addAll(sPPreview, borderSPPrev);
        
        suPieceShadow = new DropShadow();
        suPieceShadow.setOffsetX(3.0);
        suPieceShadow.setOffsetY(3.0); 
        
        setOnMouseEntered((mE)->{
            setEffect(suPieceShadow);
        });
        setOnMouseExited((mEx)->{
            setEffect(null);
        });
        setOnMousePressed((mE)->{
            setTranslateX(getTranslateX() + suPieceShadow.getOffsetX());
            setTranslateY(getTranslateY() + suPieceShadow.getOffsetY());
        });
        setOnMouseReleased((mE)->{
            setTranslateX(getTranslateX() - suPieceShadow.getOffsetX());
            setTranslateY(getTranslateY() - suPieceShadow.getOffsetY());
        });
    }    
    
    public WritableImage getShrunkPiece(){
        return shrunkPiece;
    }
    
    public double getWidthOfTheShrunkPiece(){
        return shrunkPiece.getWidth();
    }
    
    public double getHeightOfTheShrunkPiece(){
        return shrunkPiece.getHeight();
    }
    
}
