// Automatically generated event hookup file.

package tmp.sunw.beanbox;
import sunw.demo.juggler.Juggler;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

public class ___Hookup_22ca00a14c implements MouseListener, java.io.Serializable {

    public void setTarget(Juggler t) {
        target = t;
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
        target.stopJuggling();
    }

    public void mousePressed(MouseEvent arg0) {
    }

    public void mouseReleased(MouseEvent arg0) {
    }

    private Juggler target;
}
