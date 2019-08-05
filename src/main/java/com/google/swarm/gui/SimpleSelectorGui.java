package com.google.swarm.gui;

import java.awt.BorderLayout;
import java.util.*;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jorphan.gui.JLabeledChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.swarm.samplers.GCPSimpleSampler;


@GUIMenuSortOrder(12)
public abstract class SimpleSelectorGui extends AbstractSamplerGui implements ChangeListener {
	
    private static final long serialVersionUID = 234L;
    private static final Logger log = LoggerFactory.getLogger(SimpleSelectorGui.class);
    private JLabeledChoice optionLabeledChoice;
    private final ArrayList<ChangeListener> mChangeListeners = new ArrayList<>(3);
    private JPanel mainPanel;
    private ArgumentsPanel argsPanel;
    private HashMap<String, AbstractJavaSamplerClient> clientMap;
    private LinkedHashSet<String> selectorValues;
    
    public SimpleSelectorGui() {
        super();
        clientMap = initClientMap();
        selectorValues = initSelector();
        init();
    }
    
	public HashMap<String, AbstractJavaSamplerClient> initClientMap() {
		return new HashMap<String, AbstractJavaSamplerClient>();
	}
	
	public LinkedHashSet<String> initSelector() {
		return new LinkedHashSet<String>();
	}
	
    public void addChangeListener(ChangeListener pchangeListener) {
        mChangeListeners.add(pchangeListener);
    }

    @Override
    public void modifyTestElement(TestElement element) {
        super.configureTestElement(element);
    }
    
    @Override
    public TestElement createTestElement() {
        GCPSimpleSampler sampler = new GCPSimpleSampler();
        modifyTestElement(sampler);
        return sampler;
    }
    
    @Override
    public void configure(TestElement el) {
        super.configure(el);
    	AbstractJavaSamplerClient client = getClient();
        el.setProperty(new StringProperty(TestElement.TEST_CLASS, client.getClass().getName()));
        toggleArgsPanel();
    }

    @Override
    public void clearGui() {
        super.clearGui();
        argsPanel.clearGui();
    }

    @Override
    public void stateChanged(ChangeEvent evt) {
        if (evt.getSource() == optionLabeledChoice) {
            toggleArgsPanel();
        }
    }

    private void init() { 
        setLayout(new BorderLayout(0, 5));
        setBorder(makeBorder());
        add(makeTitlePanel(), BorderLayout.NORTH);

        mainPanel = new JPanel(new BorderLayout());        
        if (!selectorValues.isEmpty()) {
            mainPanel.add(createOptionPanel(), BorderLayout.NORTH);
        	mainPanel.add(createArgsPanel(selectorValues.iterator().next()));  
        }
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createOptionPanel() {
        VerticalPanel optionPanel = new VerticalPanel();
        optionLabeledChoice = new JLabeledChoice("Type ", selectorValues.toArray(new String[selectorValues.size()]), true, false);
        optionLabeledChoice.addChangeListener(this);
        optionPanel.add(optionLabeledChoice);
        return optionPanel;
    }
    
	private void toggleArgsPanel() {
		String option = optionLabeledChoice.getText().trim();
		mainPanel.remove(argsPanel);		
		mainPanel.add(createArgsPanel(option));
		mainPanel.revalidate();
		mainPanel.repaint();
	}
	
    private JPanel createArgsPanel(String queryType) {
    	argsPanel = new ArgumentsPanel(queryType);
        try {
        	JavaSamplerClient client = clientMap.get(queryType);     	
            Arguments currArgs = new Arguments();
            argsPanel.modifyTestElement(currArgs);
            Map<String, String> currArgsMap = currArgs.getArgumentsAsMap();
            Arguments newArgs = new Arguments();
            Arguments testParams = client.getDefaultParameters();
            
            if (testParams != null) {
                for (JMeterProperty jMeterProperty : testParams.getArguments()) {
                    Argument arg = (Argument) jMeterProperty.getObjectValue();
                    String name = arg.getName();
                    String value = arg.getValue();

                    // If a user has set parameters in one test, and then selects a different test which supports the same
                    // parameters, those parameters should have the same values that they did in the original test.
                    if (currArgsMap.containsKey(name)) {
                        String newVal = currArgsMap.get(name);
                        if (newVal != null && newVal.length() > 0) {
                            value = newVal;
                        }
                    }
                    newArgs.addArgument(name, value);
                }
            }
            argsPanel.configure(newArgs);
        } 
        catch (Exception e) {
            log.error(e.getMessage());
        }
        
        return argsPanel;
    }
    
    private AbstractJavaSamplerClient getClient() {
    	AbstractJavaSamplerClient client = null;
		if (optionLabeledChoice != null) {
			client = clientMap.get(optionLabeledChoice.getText().trim());
		}
		else {
			client = clientMap.get(selectorValues.iterator().next());
		}
		return client;
    }


}
