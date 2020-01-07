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

    private JTextField labelField;
    private JTextField projectField;
    private JTextField datasetField;

    public BQListenerGui() {
        super();
        init();
    }

    @Override
    public String getStaticLabel() {
        return "GCP - BigQuery Results";
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
            labelField.setText(bq.getLabelAsString());
            projectField.setText(bq.getProjectAsString());
            datasetField.setText(bq.getDatasetAsString());
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
        	bqSaver.setLabel(labelField.getText());
        	bqSaver.setProject(projectField.getText());
        	bqSaver.setDataset(datasetField.getText());

        }
    }

    @Override
    public void clearGui() {
        super.clearGui();

        labelField.setText("");
        projectField.setText("${project}");
        datasetField.setText("${bigquery.dataset}");
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
        
        JLabel labelLabel = new JLabel("Test Name: ");
        JLabel projectLabel = new JLabel("Project: ");
        JLabel datasetLabel = new JLabel("Dataset: ");

        JPanel labelSubPanel = new JPanel(new BorderLayout(5, 0));
        labelField = new JTextField("", 5);
        labelLabel.setLabelFor(labelField);
        labelSubPanel.add(labelLabel, BorderLayout.WEST);
        labelSubPanel.add(labelField, BorderLayout.CENTER);

        JPanel projectSubPanel = new JPanel(new BorderLayout(5, 0));
        projectField = new JTextField("", 5);
        projectLabel.setLabelFor(projectField);
        projectSubPanel.add(projectLabel, BorderLayout.WEST);
        projectSubPanel.add(projectField, BorderLayout.CENTER);

        JPanel datasetSubPanel = new JPanel(new BorderLayout(5, 0));
        datasetField = new JTextField("", 5);
        datasetLabel.setLabelFor(datasetField);
        datasetSubPanel.add(datasetLabel, BorderLayout.WEST);
        datasetSubPanel.add(datasetField, BorderLayout.CENTER);

        bqPanel.add(labelSubPanel);
        bqPanel.add(projectSubPanel);
        bqPanel.add(datasetSubPanel);

        return bqPanel;

    }

}
