import java.util.ArrayList;
import java.util.Timer;
import nz.sodium.*;
import swidgets.*;
import javax.swing.*;
import java.awt.*;
import java.util.TimerTask;

public class GpsGUI {
	
	
	private static JPanel trackerPanel(Stream<GpsEvent> s, Cell<double[]> range) {     
        Cell<String[]> simplifiedCell = s.map(ev -> new String[] {ev.name, "" + ev.latitude, "" +  ev.longitude}).hold(new String[] {"waiting...", "waiting...", "waiting..."});       
        Cell<String> numCell = simplifiedCell.map(v -> v[0]);
        Cell<String> latCell = simplifiedCell.map(v -> "Lat: " + v[1]);
        Cell<String> longCell = simplifiedCell.map(v -> "Long: " + v[2]);
        
        SLabel numLabel = new SLabel(numCell);
        SLabel latLabel = new SLabel(latCell);
        SLabel longLabel = new SLabel(longCell);
		        
		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		
		numLabel.setFont(numLabel.getFont().deriveFont(Font.BOLD, 20f));
		latLabel.setFont(latLabel.getFont().deriveFont(16f));
		longLabel.setFont(longLabel.getFont().deriveFont(16f));
		
		panel.add(numLabel);
        panel.add(latLabel);
        panel.add(longLabel);
		return panel;
	}

    static String eventToText(GpsEvent ev) {
        return ev.name + "," + ev.latitude + "," +ev.longitude + "," + ev.altitude;
    }

    static Stream<GpsEvent> inRange(Stream<GpsEvent> s, Cell<double[]> range) {
        return s.snapshot(range, (ev, r) -> ev.latitude >= r[0] && ev.latitude <= r[1] && ev.longitude >= r[2] && ev.longitude <= r[3] ? ev : null).filter(x -> x != null);
        }
    	
    static double distanceMetres(GpsEvent a, GpsEvent b) {
    	double metresPerDegree = 111320;
    	double dy = (b.latitude - a.latitude) * metresPerDegree;
    	double dx = (b.longitude - a.longitude) * metresPerDegree * Math.cos(Math.toRadians(b.latitude));
    	double dz = b.altitude - a.altitude;
    	return Math.sqrt(dx * dx + dy*dy + dz*dz);
    }
    
    static Stream<Double> segments(Stream<GpsEvent> s, Cell<double[]> range) {
		Stream<GpsEvent> metres = inRange(s, range).map(ev -> new GpsEvent(ev.name, ev.latitude, ev.longitude, ev.altitude * 0.3048));
		Cell<GpsEvent> previous = metres.hold(null);
		return metres.snapshot(previous, (cur, prev) -> prev == null ? null : distanceMetres(prev, cur))
				.filter(x -> x != null);
    	
    }
    
    static Cell<String> distanceCell(Stream<GpsEvent> s, Cell<double[]> range) {
		Stream<Double> segs = segments(s, range);
		
		Cell<ArrayList<double[]>> recent = segs.accum(new ArrayList<double[]>(), (seg, list) -> {
			ArrayList<double[]> next = new ArrayList<>(list);
			long now = System.currentTimeMillis();
			next.add(new double[] {now, seg});
			next.removeIf(t -> t[0] < now - (5 * 60 * 1000));
			return next;
		});
    	
    	
    	return recent.map(list -> {
    		double total = 0;
    		for(double[] e : list) {
    			total += e[1];
    		}
    		long rounded = (long) Math.ceil(total);
    		return "Dist: " + rounded + "m";
    	});
    	
    }
    
    static Cell<String> textWithClear(Stream<String> texts) {
    	StreamSink<String> clearSink = new StreamSink<>();
        Cell<String> lastText = texts.hold("");
        Timer timer = new Timer();
        texts.listen(text -> timer.schedule(new TimerTask(){
            public void run() { clearSink.send(text); }
        }, 3000));

        Stream<String> clears = clearSink
            .snapshot(lastText, (scheduled, current) -> scheduled.equals(current) ? "" : null)
            .filter(x -> x != null);

        Cell<String> latestEventCell = texts.merge(clears, (a, b) -> b).hold("waiting...");
    	
    	return latestEventCell;
    	
    }
    
    private static JPanel distancePanel(Stream<GpsEvent> s, Cell<double[]> range) {
    	Cell<String> numCell = s.map(ev -> ev.name).hold("waiting...");
        
        SLabel numLabel = new SLabel(numCell);
        SLabel distLabel = new SLabel(distanceCell(s, range));
        
		JPanel panel = new JPanel(new GridLayout(0, 1));
		panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		
		panel.add(numLabel);
        panel.add(distLabel);
        numLabel.setFont(numLabel.getFont().deriveFont(Font.BOLD, 20f));
        distLabel.setFont(distLabel.getFont().deriveFont(16f));
		return panel;
    	
    }
    
    
	public static void main(String[] args) {
		GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        
        
        Stream<GpsEvent> allEvents = streams[0];
        for (int i = 1;  i < streams.length; i++) {
        	allEvents = allEvents.merge(streams[i], (a, b) -> b);
        }

        Stream<String> eventText = allEvents.map(ev -> eventToText(ev));
        
        
        SLabel updateLabel = new SLabel(textWithClear(eventText));
           
        STextField latMin = new STextField("-90");
        STextField latMax = new STextField("90");
        STextField longMin = new STextField("-180");
        STextField longMax = new STextField("180");

        JLabel latMinLabel = new JLabel("Lat Min");
        JLabel latMaxLabel = new JLabel("Lat Max");
        JLabel longMinLabel = new JLabel("Long Min");
        JLabel longMaxLabel = new JLabel("Long Max");

        SButton setButton = new SButton("Set filter");
        JPanel controlPanel = new JPanel(new GridLayout(0,2));
        controlPanel.add(latMinLabel);
        controlPanel.add(latMin);
        controlPanel.add(latMaxLabel);
        controlPanel.add(latMax);
        controlPanel.add(longMinLabel);
        controlPanel.add(longMin);
        controlPanel.add(longMaxLabel);
        controlPanel.add(longMax);
        controlPanel.add(setButton);

        JFrame frame = new JFrame("GPS");

        Cell<double[]> rangeCell = setButton.sClicked
            .snapshot(latMin.text, latMax.text, longMin.text, longMax.text,
                (click, a, b, c, d) -> new double[] {Double.parseDouble(a), Double.parseDouble(b), Double.parseDouble(c), Double.parseDouble(d)} ).hold(new double [] {-90, 90, -180, 180});

        Cell<String> filteredCell = textWithClear(inRange(allEvents, rangeCell).map(ev -> eventToText(ev)));
        SLabel filteredLabel = new SLabel(filteredCell);
        
        Cell<String> rangeTextCell = rangeCell.map(r -> "Lat: " + r[0] + " to " + r[1] + "  Long: " + r[2] + " to " + r[3]);
        SLabel rangeLabel = new SLabel(rangeTextCell);

        JPanel trackersPanel = new JPanel(new GridLayout(2,5));
        trackersPanel.setBorder(BorderFactory.createTitledBorder("Live trackers"));
        JPanel distanceGrid = new JPanel(new GridLayout(2, 5));
        distanceGrid.setBorder(BorderFactory.createTitledBorder("Distance travelled, last 5 minutes (events in range)"));
        
        
        JPanel eventPanel = new JPanel(new GridLayout(0, 1));
        eventPanel.setBorder(BorderFactory.createTitledBorder("Latest event"));
        eventPanel.add(updateLabel);
        
        JPanel filteredPanel = new JPanel(new GridLayout(0, 1));
        filteredPanel.setBorder(BorderFactory.createTitledBorder("Filtered display (events in range)"));
        filteredPanel.add(filteredLabel);
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filter"));
        filterPanel.add(controlPanel, BorderLayout.CENTER);
        filterPanel.add(rangeLabel, BorderLayout.SOUTH);
        
        
        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.add(eventPanel);
        rightPanel.add(filteredPanel);
        JPanel middlePanel = new JPanel(new GridLayout(1, 2));
        middlePanel.add(filterPanel);
        middlePanel.add(rightPanel);
        
        for(Stream<GpsEvent> s : streams) {
        	trackersPanel.add(trackerPanel(s, rangeCell));
        	distanceGrid.add(distancePanel(s, rangeCell));
        }
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 1));
        centerPanel.add(trackersPanel);
        centerPanel.add(distanceGrid);
        
        frame.setSize(1100,500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(middlePanel, BorderLayout.SOUTH);
        frame.setVisible(true);
	}

}
