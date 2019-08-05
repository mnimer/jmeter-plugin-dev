package com.google.swarm.samplers.spanner;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.LabelAlignment;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.gui.JLabeledChoice;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * JMeter GUI used to configure the different Google Spanner query test classes
 *
 * @Author Mike Nimer
 * @Author John-Paul Zebrowski
 */
@GUIMenuSortOrder(12)
public class SpannerSDKGui extends AbstractSamplerGui implements ChangeListener {
    private static final long serialVersionUID = 234L;


    private static final String[] QUERY_TYPES = {"READ", "INSERT"};

    private JLabeledChoice queryLabeledChoice;
    private final ArrayList<ChangeListener> mChangeListeners = new ArrayList<>(3);
    private JPanel mainPanel;
    private JPanel spannerParamPanel;
    private ArgumentsPanel argsPanel;
    private Map activeFields = new HashMap();
    ReadQuery readQuery = new ReadQuery();
    InsertQuery insertQuery = new InsertQuery();

    public SpannerSDKGui() {
        super();
        init();
    }

    @Override
    public String getStaticLabel() {
        return "GCP - Spanner Query";
    }
    
	@Override
	public String getLabelResource() {
		return "spanner_sdk_request";
	}


    /**
     * Update the GUI fields with initial or saved values
     * @param el
     */
    @Override
    public void configure(TestElement el) {
        super.configure(el);
        if (el instanceof SpannerSDKSampler){
        	SpannerSDKSampler sdk = (SpannerSDKSampler) el;
            //UpdateSamplerWithActiveFieldValues(sdk);
            for (Object key : activeFields.keySet()) {
                ((JTextComponent)activeFields.get(key)).setText(sdk.getProperty(key.toString()).getStringValue());
            }
        }
    }

    /**
     * Update the sampler with gui field values
     * @param element
     */
    @Override
    public void modifyTestElement(TestElement element) {
        if (element instanceof SpannerSDKSampler) {
        	SpannerSDKSampler sdk = (SpannerSDKSampler) element;
            UpdateSamplerWithActiveFieldValues(sdk);
        }
        super.configureTestElement(element);
    }


    /**
     * Loops over list of arguments, for query type, and grabs the corresponding GUI value
     * @param sdk
     */
    private void UpdateSamplerWithActiveFieldValues(SpannerSDKSampler sdk) {
        sdk.setQueryType( queryLabeledChoice.getText().trim() );

        //get require params
        Arguments arguments = readQuery.getDefaultParameters();
        if( queryLabeledChoice.getText().trim().equals("INSERT") ){
            arguments = insertQuery.getDefaultParameters();
        }

        //pull value from gui
        for (JMeterProperty argument : arguments) {
            String name = argument.getName();
            if( activeFields.containsKey(name) ) {
                String val = ((JTextComponent) activeFields.get(name)).getText();
                sdk.setProperty(argument.getName(), val);
            }else{
                //log.warn("Field '" +name +"' not found");
            }
        }
    }


    /**
     * Create the Test Sampler
     * @return
     */
    @Override
    public TestElement createTestElement() {
        SpannerSDKSampler bq = new SpannerSDKSampler();
        //configure(bq);
        modifyTestElement(bq);
        return bq;
    }

    @Override
    public void clearGui() {
        super.clearGui();
        createTestElement();
    }

    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());


        mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createQueryTypePanel(), BorderLayout.NORTH);

        spannerParamPanel = createSpannerParamPanel("READ");
        mainPanel.add(spannerParamPanel, BorderLayout.CENTER);

        this.add(makeTitlePanel(), BorderLayout.NORTH);
        this.add(mainPanel, BorderLayout.CENTER);
    }


    public void addChangeListener(ChangeListener pchangeListener) {
        mChangeListeners.add(pchangeListener);
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        if (evt.getSource() == queryLabeledChoice) {
            configureQueryType(queryLabeledChoice.getText().trim());
        }
    }

    private void configureQueryType(String queryType) {

        mainPanel.remove(spannerParamPanel);
        spannerParamPanel = createSpannerParamPanel(queryType);

        mainPanel.add(spannerParamPanel);
        mainPanel.revalidate();
        mainPanel.repaint();
    }



    private JPanel createQueryTypePanel() {
        VerticalPanel queryTypePanel = new VerticalPanel();

        queryLabeledChoice = new JLabeledChoice("Query Type ", QUERY_TYPES, true, false);
        queryLabeledChoice.addChangeListener(this);

        queryTypePanel.add(queryLabeledChoice);
        return queryTypePanel;
    }


    private JPanel createSpannerParamPanel(String queryType) {

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        DesignGridLayout layout = new DesignGridLayout(panel);
        layout.labelAlignment(LabelAlignment.RIGHT);
        activeFields.clear();

        Arguments arguments = readQuery.getDefaultParameters();
        if( queryType.equals("INSERT") ) {
            arguments = insertQuery.getDefaultParameters();
        }


        for (JMeterProperty arg : arguments) {
            String val = ((TestElementProperty) arg).getElement().getPropertyAsString("Argument.value");
            if( arg.getName().equals("sql")){
                JSyntaxTextArea field = JSyntaxTextArea.getInstance(30, 80);
                field.setIgnoreRepaint(true);
                field.setPreferredSize(new Dimension(100, 300));
                field.setLanguage("sql");
                field.setInitialText(val);

                activeFields.put(arg.getName(), field);
                String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(arg.getName()), ' ');
                layout.row().grid(new JLabel(label)).add(field);

            } else {
                JTextField field = new JTextField(val);
                activeFields.put(arg.getName(), field);
                String label = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(arg.getName()), ' ');
                layout.row().grid(new JLabel(label +": ")).add(field);
            }
        }


        return panel;

    }


    private JTable createTable() {

        String paramTableCols[] = {"Parameter", "Value"};
        DefaultTableModel tableModel = new DefaultTableModel(paramTableCols, 0);
        //List<String[]> rows = new ArrayList<String[]>();

        String[] row = {"ROW1_param", "ROW1_value"};
        //rows.add(row);

        tableModel.addRow(row);
        return new JTable(tableModel);
    }



}
