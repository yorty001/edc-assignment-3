import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import nz.sodium.Cell;
import nz.sodium.CellSink;
import nz.sodium.StreamSink;

class GpsGUI_Test {

	@Test
	public void distanceMetres_latitudeOnly() {
		GpsEvent a = new GpsEvent("T", 0, 0, 0);
		GpsEvent b = new GpsEvent("T", 0.001, 0, 0);
		assertEquals(111.32, GpsGUI.distanceMetres(a, b), 0.001);
	}
	
	@Test
	public void distanceMetres_altitudeOnly() {
		GpsEvent a = new GpsEvent("T", 0, 0, 0);
		GpsEvent b = new GpsEvent("T", 0, 0, 10);
		assertEquals(10, GpsGUI.distanceMetres(a, b), 0.001);
	}
	
	@Test
	public void eventToText_formatsAllFields() {
		GpsEvent ev = new GpsEvent("Tracker1", 10.5, 20.5, 30.5);
		assertEquals("Tracker1,10.5,20.5,30.5", GpsGUI.eventToText(ev));
	}
	
	@Test
	public void inRange_keepsInsideAndBoundary_dropsOutside() {
		StreamSink<GpsEvent> sink = new StreamSink<>();
		CellSink<double[]> range = new CellSink<>(new double[] {10,20,100,110});
		ArrayList<GpsEvent> received = new ArrayList<>();
		GpsGUI.inRange(sink, range).listen(ev -> received.add(ev));
		
		sink.send(new GpsEvent("inside", 15, 105, 0));
		sink.send(new GpsEvent("onLatMax", 20, 105, 0));
		sink.send(new GpsEvent("outside", 20.1 ,105, 0));
		
		assertEquals(2, received.size());
		
	}
	
	@Test
	public void segments_convertsFeetTometres() {
		StreamSink<GpsEvent> sink = new StreamSink<>();
		CellSink<double[]> range = new CellSink<>(new double[] {-90,90,-180,180});
		ArrayList<Double> received = new ArrayList<>();
		GpsGUI.segments(sink, range).listen(d -> received.add(d));
		
		sink.send(new GpsEvent("T", 0, 0, 0));
		sink.send(new GpsEvent("T", 0, 0, 100));
		
		assertEquals(1, received.size());
		assertEquals(30.48, received.get(0), 0.001);
	}
	
	@Test
	public void distanceCell_roundsTotalUp() {
		StreamSink<GpsEvent> sink = new StreamSink<>();
		CellSink<double[]> range = new CellSink<>(new double[] {-90,90,-180,180});
		Cell<String> dist = GpsGUI.distanceCell(sink, range);
		
		
		sink.send(new GpsEvent("T", 0, 0, 0));
		sink.send(new GpsEvent("T", 0, 0, 100));
		
		assertEquals("Dist: 31m", dist.sample());
	}
	
	@Test
	public void textWithClear_clearsAfterDelay() throws InterruptedException {
		StreamSink<String> sink = new StreamSink<>();
		Cell<String> cell = GpsGUI.textWithClear(sink);
		
		assertEquals("waiting...", cell.sample());
		sink.send("hello");
		assertEquals("hello", cell.sample());
		Thread.sleep(3500);
		assertEquals("", cell.sample());
		
		}
}
