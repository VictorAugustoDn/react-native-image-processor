import React, { Component } from 'react';
import {
  NativeModules,
  PanResponder,
  Dimensions,
  Image,
  View,
  Animated,
} from 'react-native';
import Svg, { Polygon } from 'react-native-svg';

const dimensions = Dimensions.get('window');
const dimWidth = dimensions.width;
const dimHeight = dimensions.height;

const AnimatedPolygon = Animated.createAnimatedComponent(Polygon);

class CustomCrop extends Component {
  constructor(props) {
    super(props);
    this.state = {
      viewHeight: dimWidth * (props.height / props.width),
      height: props.height,
      width: props.width,
      image: props.initialImage,
      moving: false,
    };

    const defaultCropHeight = dimHeight - this.state.viewHeight + 250;

    this.state = {
      ...this.state,
      topLeft: new Animated.ValueXY(
        props.rectangleCoordinates
          ? this.imageCoordinatesToViewCoordinates(props.rectangleCoordinates.topLeft)
          : { x: 100, y: defaultCropHeight },
      ),
      topRight: new Animated.ValueXY(
        props.rectangleCoordinates
          ? this.imageCoordinatesToViewCoordinates(props.rectangleCoordinates.topRight)
          : { x: dimWidth - 100, y: defaultCropHeight },
      ),
      bottomLeft: new Animated.ValueXY(
        props.rectangleCoordinates
          ? this.imageCoordinatesToViewCoordinates(props.rectangleCoordinates.bottomLeft)
          : { x: 100, y: this.state.viewHeight - 100 },
      ),
      bottomRight: new Animated.ValueXY(
        props.rectangleCoordinates
          ? this.imageCoordinatesToViewCoordinates(props.rectangleCoordinates.bottomRight)
          : {
            x: dimWidth - 100,
            y: this.state.viewHeight - 100,
          },
      ),
    };
    this.state = {
      ...this.state,
      overlayPositions: `${this.state.topLeft.x._value},${
        this.state.topLeft.y._value
        } ${this.state.topRight.x._value},${this.state.topRight.y._value} ${
        this.state.bottomRight.x._value
        },${this.state.bottomRight.y._value} ${
        this.state.bottomLeft.x._value
        },${this.state.bottomLeft.y._value}`,
    };

    this.panResponderTopLeft = this.createPanResponser(this.state.topLeft, 'topLeft');
    this.panResponderTopRight = this.createPanResponser(this.state.topRight, 'topRight');
    this.panResponderBottomLeft = this.createPanResponser(this.state.bottomLeft, 'bottomLeft');
    this.panResponderBottomRight = this.createPanResponser(this.state.bottomRight, 'bottomRight');
  }

  createPanResponser(corner, position) {
    return PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onPanResponderMove: Animated.event([
        null,
        {
          dx: corner.x,
          dy: corner.y,
        },
      ], {
        useNativeDriver: false,
        listener: (event, gestureState) => {
          let _tlx = this.state.topLeft.x._value;
          let _tly = this.state.topLeft.y._value;
          let _trx = this.state.topRight.x._value;
          let _try = this.state.topRight.y._value;
          let _blx = this.state.bottomLeft.x._value;
          let _bly = this.state.bottomLeft.y._value;
          let _brx = this.state.bottomRight.x._value;
          let _bry = this.state.bottomRight.y._value;

          const { moveX, moveY } = gestureState;

          switch (position) {
            case 'topLeft':
              _tlx = moveX;
              _tly = moveY;
              break;
            case 'topRight':
              _trx = moveX;
              _try = moveY;
              break;
            case 'bottomLeft':
              _blx = moveX;
              _bly = moveY;
              break;
            case 'bottomRight':
              _brx = moveX;
              _bry = moveY;
              break;
            default:
              break;
          }

          this.setState({
            overlayPositions: `${_tlx},${_tly} ${_trx},${_try} ${_brx},${_bry} ${_blx},${_bly}`,
          });
        }
      }),
      onPanResponderRelease: () => {
        corner.flattenOffset();
        this.updateOverlayString();
      },
      onPanResponderGrant: () => {
        corner.setOffset({ x: corner.x._value, y: corner.y._value });
        corner.setValue({ x: 0, y: 0 });
      },
    });
  }

  crop() {
    const coordinates = {
      topLeft: this.viewCoordinatesToImageCoordinates(this.state.topLeft),
      topRight: this.viewCoordinatesToImageCoordinates(
        this.state.topRight,
      ),
      bottomLeft: this.viewCoordinatesToImageCoordinates(
        this.state.bottomLeft,
      ),
      bottomRight: this.viewCoordinatesToImageCoordinates(
        this.state.bottomRight,
      ),
      height: this.state.height,
      width: this.state.width,
    };
    NativeModules.CustomCropManager.crop(
      coordinates,
      this.state.image,
      (err, res) => this.props.updateImage(res, coordinates),
    );
  }

  updateOverlayString() {
    this.setState({
      overlayPositions: `${this.state.topLeft.x._value},${
        this.state.topLeft.y._value
        } ${this.state.topRight.x._value},${this.state.topRight.y._value} ${
        this.state.bottomRight.x._value
        },${this.state.bottomRight.y._value} ${
        this.state.bottomLeft.x._value
        },${this.state.bottomLeft.y._value}`,
    });
  }

  imageCoordinatesToViewCoordinates(corner) {
    return {
      x: (corner.x * dimWidth) / this.state.width,
      y: (corner.y * this.state.viewHeight) / this.state.height,
    };
  }

  viewCoordinatesToImageCoordinates(corner) {
    return {
      x:
        (corner.x._value / dimWidth) *
        this.state.width,
      y: (corner.y._value / this.state.viewHeight) * this.state.height,
    };
  }

  render() {
    return (
      <View
        style={{
          flex: 1,
          alignItems: 'center',
          justifyContent: 'flex-end',
        }}
      >
        <View
          style={[
            s(this.props).cropContainer,
            { height: this.state.viewHeight },
          ]}
        >
          <Image
            style={[
              s(this.props).image,
              { height: this.state.viewHeight },
            ]}
            resizeMode="contain"
            source={{ uri: this.state.image }}
          />
          <Svg
            height={this.state.viewHeight}
            width={dimWidth}
            style={{ position: 'absolute', left: 0, top: 0 }}
          >
            <AnimatedPolygon
              ref={(ref) => (this.polygon = ref)}
              fill={this.props.overlayColor || '#4c50da'}
              fillOpacity={this.props.overlayOpacity || 0.5}
              stroke={this.props.overlayStrokeColor || '#4c50da'}
              points={this.state.overlayPositions}
              strokeWidth={this.props.overlayStrokeWidth || 3}
            />
          </Svg>
          <Animated.View
            {...this.panResponderTopLeft.panHandlers}
            style={[
              this.state.topLeft.getLayout(),
              s(this.props).handler,
            ]}
          >
            <View
              style={[
                s(this.props).handlerRound,
                { left: 5, top: 5 },
              ]}
            />
          </Animated.View>
          <Animated.View
            {...this.panResponderTopRight.panHandlers}
            style={[
              this.state.topRight.getLayout(),
              s(this.props).handler,
            ]}
          >
            <View
              style={[
                s(this.props).handlerRound,
                { right: 5, top: 5 },
              ]}
            />
          </Animated.View>
          <Animated.View
            {...this.panResponderBottomLeft.panHandlers}
            style={[
              this.state.bottomLeft.getLayout(),
              s(this.props).handler,
            ]}
          >
            <View
              style={[
                s(this.props).handlerRound,
                { left: 5, bottom: 5 },
              ]}
            />
          </Animated.View>
          <Animated.View
            {...this.panResponderBottomRight.panHandlers}
            style={[
              this.state.bottomRight.getLayout(),
              s(this.props).handler,
            ]}
          >
            <View
              style={[
                s(this.props).handlerRound,
                { right: 5, bottom: 5 },
              ]}
            />
          </Animated.View>
        </View>
      </View>
    );
  }
}

const s = (props) => ({
  image: {
    width: dimWidth,
    position: 'absolute',
  },
  handlerI: {
    // borderRadius: 0,
    // height: 20,
    // width: 20,
    // backgroundColor: props.handlerColor || '#4c50da',
  },
  handlerRound: {
    width: 20,
    height: 20,
    margin: 15,
    position: 'absolute',
    borderRadius: 100,
    backgroundColor: props.handlerColor || 'transparent',
    borderWidth: 3,
    borderColor: props.handlerBorderColor || '#4c50da',
  },
  handler: {
    height: 60,
    width: 60,
    overflow: 'visible',
    marginLeft: -30,
    marginTop: -30,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'absolute',
  },
  cropContainer: {
    position: 'absolute',
    left: 0,
    width: dimWidth,
    top: 0,
  },
});

export default CustomCrop;
export const ImageManupulator = NativeModules.CustomCropManager;
