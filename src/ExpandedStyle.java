
import java.awt.Color;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleConstants.CharacterConstants;

public class ExpandedStyle {
  public boolean b, u, i;
  public int s;
  public Color c;

  public ExpandedStyle(SimpleAttributeSet set) {

    try {
      b = (boolean) set.getAttribute(CharacterConstants.Bold);
    } catch (NullPointerException e) {
      b = false;
    }

    try {
      i = (boolean) set.getAttribute(CharacterConstants.Italic);
    } catch (NullPointerException e) {
      i = false;
    }

    try {
      u = (boolean) set.getAttribute(CharacterConstants.Underline);
    } catch (NullPointerException e) {
      u = false;
    }
    try {
      c = (Color) set.getAttribute(StyleConstants.Foreground);
    } catch (NullPointerException e) {
      c = Color.black;
    }
    try {
      s = (int) set.getAttribute(CharacterConstants.Size);
    } catch (NullPointerException e) {
      s = 12;
    }

  }
}
