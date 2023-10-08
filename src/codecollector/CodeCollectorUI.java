package codecollector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class CodeCollectorUI extends JFrame {
	private static final long serialVersionUID = -8193408663420970170L;
	private File sourceDirectory;
	private File outputFile;
	private JLabel sourceLabel;
	private JLabel outputLabel;
	private JProgressBar progressBar;
	private JLabel filesLabel;

	public CodeCollectorUI() {
		super("CodeCollector by Vainiven");

		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().setBackground(Color.DARK_GRAY);

		JButton sourceButton = new JButton("Select Source Directory");
		sourceButton.addActionListener(this::selectSourceDirectory);
		sourceButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton outputButton = new JButton("Select Output File");
		outputButton.addActionListener(this::selectOutputFile);
		outputButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		JButton collectButton = new JButton("Collect");
		collectButton.addActionListener(this::collectCode);
		collectButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		sourceLabel = new JLabel("Source Directory: None");
		sourceLabel.setForeground(Color.WHITE);
		outputLabel = new JLabel("Output File: None");
		outputLabel.setForeground(Color.WHITE);
		filesLabel = new JLabel("");
		filesLabel.setForeground(Color.WHITE);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

		add(sourceButton);
		add(sourceLabel);
		add(Box.createRigidArea(new Dimension(0, 5)));
		add(outputButton);
		add(outputLabel);
		add(Box.createRigidArea(new Dimension(0, 5)));
		add(collectButton);
		add(progressBar);
		add(filesLabel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(500, 200);

		// Set default output file to user's desktop with filename "CodeCollector.txt"
		String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
		outputFile = new File(desktopPath, "CodeCollector.txt");
		outputLabel.setText("Output File: " + outputFile.getAbsolutePath());
	}

	private void selectSourceDirectory(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			sourceDirectory = chooser.getSelectedFile();
			sourceLabel.setText("Source Directory: " + sourceDirectory.getAbsolutePath());
			pack();
		}
	}

	private void selectOutputFile(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		// Set default file name
		chooser.setSelectedFile(outputFile);
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			outputFile = chooser.getSelectedFile();
			outputLabel.setText("Output File: " + outputFile.getAbsolutePath());
			pack();
		}
	}

	private void collectCode(ActionEvent e) {
		if (sourceDirectory == null || outputFile == null) {
			JOptionPane.showMessageDialog(this, "Please select both source directory and output file.");
			return;
		}

		progressBar.setValue(0);
		progressBar.setString(null);
		progressBar.setVisible(true);
		filesLabel.setText("");
		pack();

		Executors.newSingleThreadExecutor().execute(() -> {
			try (OutputStream writer = new BufferedOutputStream(new FileOutputStream(outputFile))) {
				AtomicLong javaFiles = new AtomicLong();
				AtomicLong classFiles = new AtomicLong();

				long totalFiles = Files.walk(sourceDirectory.toPath()).filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java")).peek(path -> {
							if (path.toString().endsWith(".java")) {
								javaFiles.incrementAndGet();
							} else {
								classFiles.incrementAndGet();
							}
						}).count();

				long[] processedFiles = { 0 };

				Files.walk(sourceDirectory.toPath()).filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
							try {
								byte[] bytes = Files.readAllBytes(path);
								writer.write(bytes);
								writer.write(System.lineSeparator().getBytes()); // Add a newline after each file
								processedFiles[0]++;
								int progress = (int) (100 * processedFiles[0] / totalFiles);
								SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
							} catch (IOException ex) {
								throw new UncheckedIOException(ex);
							}
						});

				SwingUtilities.invokeLater(() -> filesLabel.setText(String
						.format("Processed %d .java files and %d .class files", javaFiles.get(), classFiles.get())));
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage());
			}

			SwingUtilities.invokeLater(() -> progressBar.setString("DONE"));
		});
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new CodeCollectorUI().setVisible(true);
		});
	}
}