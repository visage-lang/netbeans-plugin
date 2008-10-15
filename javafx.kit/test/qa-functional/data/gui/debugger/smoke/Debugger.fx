package smokedebugger;

import javafx.stage.*;

import javafx.scene.*;

import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;


import javafx.animation.*;

import java.lang.Math;

var radius = 50;

var angle = 0.0;
var frequency = 0.05;

var timeline = Timeline {
    repeatCount: Timeline.INDEFINITE
    keyFrames : [
        KeyFrame {
            time : 1s
            values: angle => 360.0 tween Interpolator.LINEAR
        }
    ]
}

timeline.play();

Stage {
    title: "Circle"
    width: 300
    height: 300
    onClose: function() {  java.lang.System.exit( 0 ); }
    visible: true

    scene: Scene{
        content: [
            Circle {
              transforms: Translate { x : 150, y : 150 }
              centerX: bind radius * Math.cos(frequency * angle)
              centerY: bind radius * Math.sin(frequency * angle)
              radius: 10
              fill: Color.ORANGE
            }
        ]
    }
}