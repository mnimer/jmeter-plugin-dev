package com.google.swarm.listeners.bigquery;

import java.awt.BorderLayout;

import javax.swing.*;

import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.gui.AbstractListenerGui;


/**
 * GUI to configure the Google BigQuery listener
 *
 * @Author John-Paul Zebrowski
 */
@GUIMenuSortOrder(12)
public class BQListenerGui extends AbstractListenerGui {
    private static final long serialVersionUID = 234L;

    private JTextField datasetField;
    private JTextField tableField;

    public BQListenerGui() {
        super();
        init();
    }

    @Override
    public String getStaticLabel() {
        return "GCP BigQuery Results";
    }
    
	@Override
	public String getLabelResource() {
		return "bq_result_saver";
	}

    @Override
    public void configure(TestElement el) {
        super.configure(el);
        if (el instanceof BQListener){
        	BQListener bq = (BQListener) el;
            //showScopeSettings(bq, false);
            datasetField.setText(bq.getDatasetAsString());
            tableField.setText(bq.getTableAsString());
        }
    }

    @Override
    public TestElement createTestElement() {
        BQListener bq = new BQListener();
        modifyTestElement(bq);
        return bq;
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
        if (element instanceof BQListener) {
        	BQListener bqSaver = (BQListener) element;
            // saveScopeSettings(bqProcessor);
        	bqSaver.setDataset(datasetField.getText());
        	bqSaver.setTable(tableField.getText());
        }
    }

    @Override
    public void clearGui() {
        super.clearGui();
        
        datasetField.setText(""); 
        tableField.setText("");
    }

    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);
        //add(createScopePanel(false));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createBigQueryPanel(), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createBigQueryPanel() {
        VerticalPanel bqPanel = new VerticalPanel();
        
        JLabel datasetLabel = new JLabel("Dataset "); 
        JLabel tableLabel = new JLabel("Table ");
        
        JPanel datasetSubPanel = new JPanel(new BorderLayout(5, 0));
        datasetField = new JTextField("", 5); 
        datasetLabel.setLabelFor(datasetField);
        datasetSubPanel.add(datasetLabel, BorderLayout.WEST);
        datasetSubPanel.add(datasetField, BorderLayout.CENTER);

        JPanel tableSubPanel = new JPanel(new BorderLayout(5, 0));
        tableField = new JTextField("", 5); 
        tableLabel.setLabelFor(tableField);
        tableSubPanel.add(tableLabel, BorderLayout.WEST);
        tableSubPanel.add(tableField, BorderLayout.CENTER);
        
        bqPanel.add(datasetSubPanel);
        bqPanel.add(tableSubPanel);

        return bqPanel;

    }

}
