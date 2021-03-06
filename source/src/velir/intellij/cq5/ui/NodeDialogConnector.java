package velir.intellij.cq5.ui;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBScrollPane;
import velir.intellij.cq5.jcr.model.VNode;
import velir.intellij.cq5.jcr.model.VNodeDefinition;
import velir.intellij.cq5.util.Anonymous;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class NodeDialogConnector {
	final JPanel propertiesPanel = new JPanel(new VerticalFlowLayout());
	final JPanel rootPanel = new JPanel(new VerticalFlowLayout());

	private VNode vNode;
	private boolean canChangeName;
	private boolean canChangeType;

	public NodeDialogConnector (boolean canChangeName, boolean canChangeType, VNode vNode) {
		this.canChangeName = canChangeName;
		this.canChangeType = canChangeType;
		this.vNode = vNode;
		final VNode finalNode = this.vNode;

		// node name
		JPanel namePanel = new JPanel(new GridLayout(1,2));
		JLabel nameLabel = new JLabel("name");
		namePanel.add(nameLabel);
		final JTextField nameField = new JTextField(vNode.getName());
		new DocumentListenerAdder(nameField,  new Callback<String>() {
			public void process(String s) {
				finalNode.setName(s);
			}
		});
		nameField.setEditable(canChangeName);
		namePanel.add(nameField);
		rootPanel.add(namePanel);

		// if we could not connect to the JCR, display warning
		if (! VNodeDefinition.hasDefinitions()) {
			JLabel jLabel = new JLabel("warning: could not connect to JCR to fetch definitions");
			jLabel.setForeground(Color.RED);
			rootPanel.add(jLabel);
		}

		// node primary type
		JPanel primaryTypePanel = new JPanel(new GridLayout(1,2));
		JLabel primaryTypeLabel = new JLabel("type");
		primaryTypePanel.add(primaryTypeLabel);
		// only allow selecting of node type on node creation
		if (canChangeType) {
			// add selector for primaryType if we could connect to the JCR and built definitions
			if (VNodeDefinition.hasDefinitions()) {
				final JComboBox jComboBox = new JComboBox(VNodeDefinition.getNodeTypeNames());
				jComboBox.setSelectedItem(getProperty(VNode.JCR_PRIMARYTYPE, String.class));
				jComboBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String newPrimaryType = (String) jComboBox.getSelectedItem();
						changeNodeType(newPrimaryType);
					}
				});
				primaryTypePanel.add(jComboBox);
			}
			// if we couldn't connect to the JCR, just allow the user to put anything in for primary type
			else {
				JTextField primaryTypeField = new JTextField(getProperty(VNode.JCR_PRIMARYTYPE, String.class));
				new DocumentListenerSingleAdder(VNode.JCR_PRIMARYTYPE, primaryTypeField, new Anonymous<String, Object>() {
					public Object call(String s) {
						return s;
					}
				});
				primaryTypePanel.add(primaryTypeField);
			}
		} else {
			JTextField primaryTypeField = new JTextField(getProperty(VNode.JCR_PRIMARYTYPE, String.class));
			primaryTypeField.setEditable(false);
			primaryTypePanel.add(primaryTypeField);
		}
		rootPanel.add(primaryTypePanel);

		// separator
		rootPanel.add(new JSeparator(JSeparator.HORIZONTAL));

		// properties
		populatePropertiesPanel();
		JBScrollPane jbScrollPane = new JBScrollPane(propertiesPanel,
				JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jbScrollPane.setPreferredSize(new Dimension(400, 500));
		rootPanel.add(jbScrollPane);

		// separator
		rootPanel.add(new JSeparator(JSeparator.HORIZONTAL));

		// make add property panel
		JPanel newPropertyPanel = new JPanel(new GridLayout(1,2));
		final JTextField jTextField = new JTextField();
		newPropertyPanel.add(jTextField);
		final JComboBox addPropertyCombo = new JComboBox(VNode.TYPESTRINGS);
		newPropertyPanel.add(addPropertyCombo);
		final JButton jButton = new JButton("add property");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String type = (String) addPropertyCombo.getSelectedItem();
				if (VNode.BOOLEAN_PREFIX.equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), false);
				} else if (VNode.LONG_PREFIX.equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), 0L);
				} else if (VNode.DOUBLE_PREFIX.equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), 0.0D);
				} else if ((VNode.LONG_PREFIX + "[]").equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), new Long[] {0L});
				} else if ((VNode.DOUBLE_PREFIX + "[]").equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), new Double[] {0.0D});
				} else if ((VNode.BOOLEAN_PREFIX + "[]").equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), new Boolean[] {false});
				} else if ("{String}[]".equals(type)) {
					addPropertyPanel(propertiesPanel, jTextField.getText(), new String[] {""});
				} else {
					addPropertyPanel(propertiesPanel, jTextField.getText(), "");
				}
				propertiesPanel.revalidate();
			}
		});
		newPropertyPanel.add(jButton);
		new DocumentListenerAdder(jTextField, new Callback<String>() {
			public void process(String s) {
				jButton.setEnabled(! hasProperty(s));
			}
		});
		rootPanel.add(newPropertyPanel);
	}

	private void changeNodeType (String type) {
		vNode = new VNode(vNode.getName(), type);
		populatePropertiesPanel();
	}

	private void populatePropertiesPanel () {
		propertiesPanel.removeAll();

		for (String key : vNode.getSortedPropertyNames()) {
			// don't add another primaryType selector
			if (! VNode.JCR_PRIMARYTYPE.equals(key)) {
				addPropertyPanel(propertiesPanel, key, getProperty(key));
			}
		}
		propertiesPanel.revalidate();
	}

	private <T> T getProperty (String name, Class<T> type) {
		return vNode.getProperty(name, type);
	}

	private Object getProperty (String name) {
		return vNode.getProperty(name);
	}

	private void setProperty (String name, Object o) {
		vNode.setProperty(name, o);
	}

	private boolean hasProperty (String name) {
		return vNode.hasProperty(name);
	}

	public boolean canAlter(String name) {
		return vNode.canAlter(name);
	}

	public void removeProperty(String name) {
		vNode.removeProperty(name);
	}

	public boolean canRemove(String name) {
		return vNode.canRemove(name);
	}

	interface Callback<T> {
		public void process(T t);
	}

	// convenience class for adding document listeners
	class DocumentListenerAdder {
		public DocumentListenerAdder (final JTextField jTextField, final Callback<String> callback) {
			jTextField.getDocument().addDocumentListener(new DocumentListener() {
				public void insertUpdate(DocumentEvent e) {
					callback.process(jTextField.getText());
				}

				public void removeUpdate(DocumentEvent e) {
					callback.process(jTextField.getText());
				}

				public void changedUpdate(DocumentEvent e) {
					callback.process(jTextField.getText());
				}
			});
		}
	}

	// convenience class for adding single-valued document listeners
	class DocumentListenerSingleAdder {
		public DocumentListenerSingleAdder (final String name, final JTextField jTextField, final Anonymous<String, Object> makeObject) {
			new DocumentListenerAdder(jTextField, new Callback<String>() {
				public void process(String s) {
					setProperty(name, makeObject.call(s));
				}
			});
		}
	}

	private void addMultiValueProperty(JPanel jPanel, final String name, Object o) {

		if (o instanceof Long[]) {
			final Set<RegexTextField> inputs = new HashSet<RegexTextField>();
			final JPanel outerPanel = new JPanel(new VerticalFlowLayout());
			final JPanel valuesPanel = new JPanel(new VerticalFlowLayout());

			final Runnable setPropertyValues = new Runnable() {
				public void run() {
					Long[] values = new Long[inputs.size()];
					int i = 0;
					for (RegexTextField input : inputs) {
						values[i++] = Long.parseLong(input.getText());
					}
					setProperty(name, values);
				}
			};

			final Callback<Long> addValuePanel = new Callback<Long>() {
				public void process(Long aLong) {
					final JPanel innerPanel = new JPanel(new FlowLayout());

					final RegexTextField regexTextField = new RegexTextField(Pattern.compile("[0-9]*"), aLong.toString());
					new DocumentListenerAdder(regexTextField, new Callback<String>() {
						public void process(String s) {
							setPropertyValues.run();
						}
					});
					inputs.add(regexTextField);
					innerPanel.add(regexTextField);

					// remove value button
					JButton jButton = new JButton("X");
					jButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							inputs.remove(regexTextField);
							valuesPanel.remove(innerPanel);
							valuesPanel.revalidate();

							// set property now that value has been removed
							setPropertyValues.run();
						}
					});
					innerPanel.add(jButton);

					valuesPanel.add(innerPanel);
				}
			};

			// add a panel for each value of values array
			for (Long lo : (Long[]) o ) {
				addValuePanel.process(lo);
			}
			outerPanel.add(valuesPanel);

			// add new value button
			JButton jButton = new JButton("Add");
			jButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addValuePanel.process(0L);
					setPropertyValues.run();
					valuesPanel.revalidate();
				}
			});
			outerPanel.add(jButton);

			jPanel.add(outerPanel);
		}
		else if (o instanceof Double[]) {
			final Set<RegexTextField> inputs = new HashSet<RegexTextField>();
			final JPanel outerPanel = new JPanel(new VerticalFlowLayout());
			final JPanel valuesPanel = new JPanel(new VerticalFlowLayout());

			final Runnable setPropertyValues = new Runnable() {
				public void run() {
					Double[] values = new Double[inputs.size()];
					int i = 0;
					for (RegexTextField input : inputs) {
						values[i++] = Double.parseDouble(input.getText());
					}
					setProperty(name, values);
				}
			};

			final Callback<Double> addValuePanel = new Callback<Double>() {
				public void process(Double aDouble) {
					final JPanel innerPanel = new JPanel(new FlowLayout());

					final RegexTextField regexTextField = new RegexTextField(Pattern.compile("[0-9]*\\.?[0-9]*"), aDouble.toString());
					new DocumentListenerAdder(regexTextField, new Callback<String>() {
						public void process(String s) {
							setPropertyValues.run();
						}
					});
					inputs.add(regexTextField);
					innerPanel.add(regexTextField);

					// remove value button
					JButton jButton = new JButton("X");
					jButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							inputs.remove(regexTextField);
							valuesPanel.remove(innerPanel);
							valuesPanel.revalidate();

							// set property now that value has been removed
							setPropertyValues.run();
						}
					});
					innerPanel.add(jButton);

					valuesPanel.add(innerPanel);
				}
			};

			// add a panel for each value of values array
			for (Double lo : (Double[]) o ) {
				addValuePanel.process(lo);
			}
			outerPanel.add(valuesPanel);

			// add new value button
			JButton jButton = new JButton("Add");
			jButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addValuePanel.process(0.0D);
					setPropertyValues.run();
					valuesPanel.revalidate();
				}
			});
			outerPanel.add(jButton);

			jPanel.add(outerPanel);
		}
		else if (o instanceof Boolean[]) {
			final Set<JCheckBox> inputs = new HashSet<JCheckBox>();
			final JPanel outerPanel = new JPanel(new VerticalFlowLayout());
			final JPanel valuesPanel = new JPanel(new VerticalFlowLayout());

			final Runnable setPropertyValues = new Runnable() {
				public void run() {
					Boolean[] values = new Boolean[inputs.size()];
					int i = 0;
					for (JCheckBox input : inputs) {
						values[i++] = input.isSelected();
					}
					setProperty(name, values);
				}
			};

			final Callback<Boolean> addValuePanel = new Callback<Boolean>() {
				public void process(Boolean aBoolean) {
					final JPanel innerPanel = new JPanel(new FlowLayout());

					final JCheckBox jCheckBox = new JCheckBox("", aBoolean);
					jCheckBox.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							setPropertyValues.run();
						}
					});
					inputs.add(jCheckBox);
					innerPanel.add(jCheckBox);

					// remove value button
					JButton jButton = new JButton("X");
					jButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							inputs.remove(jCheckBox);
							valuesPanel.remove(innerPanel);
							valuesPanel.revalidate();

							// set property now that value has been removed
							setPropertyValues.run();
						}
					});
					innerPanel.add(jButton);

					valuesPanel.add(innerPanel);
				}
			};

			// add a panel for each value of values array
			for (Boolean bo : (Boolean[]) o ) {
				addValuePanel.process(bo);
			}
			outerPanel.add(valuesPanel);

			// add new value button
			JButton jButton = new JButton("Add");
			jButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addValuePanel.process(false);
					setPropertyValues.run();
					valuesPanel.revalidate();
				}
			});
			outerPanel.add(jButton);

			jPanel.add(outerPanel);
		}
		else if (o instanceof String[]) {
			final Set<JTextField> inputs = new HashSet<JTextField>();
			final JPanel outerPanel = new JPanel(new VerticalFlowLayout());
			final JPanel valuesPanel = new JPanel(new VerticalFlowLayout());

			final Runnable setPropertyValues = new Runnable() {
				public void run() {
					String[] values = new String[inputs.size()];
					int i = 0;
					for (JTextField input : inputs) {
						values[i++] = input.getText();
					}
					setProperty(name, values);
				}
			};

			final Callback<String> addValuePanel = new Callback<String>() {
				public void process(String s) {
					final JPanel innerPanel = new JPanel(new FlowLayout());

					final JTextField jTextField = new JTextField(s);
					jTextField.setPreferredSize(RegexTextField.GOOD_SIZE);
					new DocumentListenerAdder(jTextField, new Callback<String>() {
						public void process(String s) {
							setPropertyValues.run();
						}
					});
					inputs.add(jTextField);
					innerPanel.add(jTextField);

					// remove value button
					JButton jButton = new JButton("X");
					jButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							inputs.remove(jTextField);
							valuesPanel.remove(innerPanel);
							valuesPanel.revalidate();

							// set property now that value has been removed
							setPropertyValues.run();
						}
					});
					innerPanel.add(jButton);

					valuesPanel.add(innerPanel);
				}
			};

			// add a panel for each value of values array
			for (String s : (String[]) o ) {
				addValuePanel.process(s);
			}
			outerPanel.add(valuesPanel);

			// add new value button
			JButton jButton = new JButton("Add");
			jButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addValuePanel.process("");
					setPropertyValues.run();
					valuesPanel.revalidate();
				}
			});
			outerPanel.add(jButton);

			jPanel.add(outerPanel);
		}
	}

	private void addPropertyPanel (final JPanel parentPanel, final String name, final Object value) {

		final JPanel jPanel = new JPanel(new GridLayout(1,3));

		// make sure the property is set in the node
		setProperty(name, value);

		// make label
		JLabel jLabel = new JLabel(name);
		jPanel.add(jLabel);

		// make input based on property class
		if (value instanceof Boolean) {
			final JCheckBox jCheckBox = new JCheckBox("", (Boolean) value);
			jCheckBox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setProperty(name, jCheckBox.isSelected());
				}
			});
			jCheckBox.setEnabled(canAlter(name));
			jPanel.add(jCheckBox);
		} else if (value instanceof Double) {
			final RegexTextField regexTextField = new RegexTextField(Pattern.compile("[0-9]*\\.?[0-9]*"), value.toString());
			new DocumentListenerSingleAdder(name,regexTextField,new Anonymous<String, Object>() {
				public Object call(String s) {
					return Double.parseDouble(s);
				}
			});
			regexTextField.setEditable(canAlter(name));
			jPanel.add(regexTextField);
		} else if (value instanceof Long) {
			final RegexTextField regexTextField = new RegexTextField(Pattern.compile("[0-9]*"), value.toString());
			new DocumentListenerSingleAdder(name, regexTextField, new Anonymous<String, Object>() {
				public Object call(String s) {
					return Long.parseLong(s);
				}
			});
			regexTextField.setEditable(canAlter(name));
			jPanel.add(regexTextField);
		} else if (value instanceof Long[]
				|| value instanceof Double[]
				|| value instanceof Boolean[]
				|| value instanceof String[]) {
			addMultiValueProperty(jPanel, name, value);
		} else {
			final JTextField jTextField = new JTextField(value.toString());
			jTextField.setPreferredSize(RegexTextField.GOOD_SIZE);
			new DocumentListenerSingleAdder(name, jTextField, new Anonymous<String, Object>() {
				public Object call(String s) {
					return s;
				}
			});
			jTextField.setEditable(canAlter(name));
			jPanel.add(jTextField);
		}

		// make remove button
		JButton jButton = new JButton("remove");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				parentPanel.remove(jPanel);
				parentPanel.revalidate();
				parentPanel.repaint();
				removeProperty(name);
			}
		});
		jButton.setEnabled(canRemove(name));
		jPanel.add(jButton);

		parentPanel.add(jPanel);
	}

	public JPanel getRootPanel() {
		return rootPanel;
	}
}
