# FX-7000G BASIC — Display Specification

## 1. Philosophy

The BASIC display should respect the character of the original FX-7000G while taking advantage of the tribute's actual screen.

Do not artificially constrain the implementation to the original calculator's display limitations.

The display has two logical domains:

1. BASIC text display
2. BASIC graphics display

## 2. Status area

PROG mode should eventually replace the normal angle-mode indicator:

    DEG

with:

    PRG

A program-slot status line should show:

    P0123456789

An occupied slot is represented by `*`.

Example:

    P0*23456789

This should be implemented as a reusable status widget rather than hard-coded BASIC drawing.

## 3. Text display

The BASIC text renderer should use a logical character grid.

The physical number of columns/rows should be determined by the current display/font configuration.

The BASIC runtime should not need to know the actual pixel dimensions.

## 4. Scrolling

If a BASIC line is wider than the visible text area:

- horizontal scrolling may be used in INPUT/editor mode
- execution output should preferably wrap or scroll vertically

The editor must retain the complete logical line.

## 5. LOCATE

LOCATE operates on the logical text grid.

Conceptual:

    LOCATE X,Y
    PRINT "HELLO"

X and Y are text-cell coordinates.

The runtime calls a display service rather than drawing pixels directly.

## 6. Graphics coordinate system

Graphics use a logical pixel coordinate system.

Origin:

    (0,0)

Recommended orientation:

    +X → right
    +Y → down

The logical coordinate limits are supplied by the display subsystem.

For example:

    SCREEN_WIDTH
    SCREEN_HEIGHT

may eventually be exposed to BASIC.

## 7. PLOT

Conceptual:

    PLOT X,Y

Sets one logical graphics pixel.

## 8. LINE

Conceptual:

    LINE X1,Y1,X2,Y2

Draws a line using the display subsystem's line primitive.

## 9. DRAW

Conceptual:

    DRAW X,Y

May be implemented as relative drawing from the current graphics cursor.

Exact semantics should be finalized before implementation.

## 10. BOX

Proposed FX-7000G extension:

    BOX X1,Y1,X2,Y2

Draws a rectangle.

This is intentionally not required to match an historical FX-880P command.

## 11. CIRCLE

Proposed FX-7000G extension:

    CIRCLE X,Y,R

Draws a circle.

It can be added after the core graphics primitives work.

## 12. CLS

CLS clears the BASIC display/output area.

The exact relationship between text and graphics buffers should be defined during implementation.

## 13. Display architecture

Recommended abstraction:

    BasicDisplay
        ├── clear()
        ├── print()
        ├── locate()
        ├── scroll()
        ├── plot()
        ├── line()
        ├── draw()
        ├── box()
        └── circle()

The BASIC runtime should call this abstraction rather than manipulating UI widgets directly.

## 14. Graphics/text interaction

Initially, graphics and text may share one display surface.

Later, a dual-buffer model may be introduced if useful:

    text/output buffer
    graphics buffer

The implementation should not assume this from the beginning.

## 15. Compatibility principle

Display commands are part of the FX-7000G BASIC design.

Historical behaviour is useful inspiration, but the tribute's display should be allowed to do more.
