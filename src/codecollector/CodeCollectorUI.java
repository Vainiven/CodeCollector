package codecollector;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class CodeCollectorUI extends JFrame {

	/**
	 * @author Vainiven https://github.com/Vainiven
	 */

	private static final long serialVersionUID = 1L;
	private File sourceDirectory;
	private File outputFile;
	private JLabel sourceLabel;
	private JLabel outputLabel;
	private JProgressBar progressBar;

	public CodeCollectorUI() {
		JButton sourceButton = new JButton("Select Source Directory");
		sourceButton.addActionListener(this::selectSourceDirectory);

		JButton outputButton = new JButton("Select Output File");
		outputButton.addActionListener(this::selectOutputFile);

		JButton collectButton = new JButton("Collect");
		collectButton.addActionListener(this::collectCode);

		sourceLabel = new JLabel("Source Directory: None");
		outputLabel = new JLabel("Output File: None");

		progressBar = new JProgressBar(0, 100);
		progressBar.setVisible(false);

		setLayout(new FlowLayout());

		add(sourceButton);
		add(sourceLabel);
		add(outputButton);
		add(outputLabel);
		add(collectButton);
		add(progressBar);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
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
		progressBar.setVisible(true);
		pack();

		Executors.newSingleThreadExecutor().execute(() -> {
			try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE)) {
				long totalFiles = Files.walk(sourceDirectory.toPath()).filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java")).count();

				long[] processedFiles = { 0 };

				Files.walk(sourceDirectory.toPath()).filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
							try {
								Files.lines(path, StandardCharsets.UTF_8).forEach(line -> {
									try {
										writer.write(line);
										writer.newLine();
									} catch (IOException ex) {
										throw new UncheckedIOException(ex);
									}
								});
								processedFiles[0]++;
								int progress = (int) (100 * processedFiles[0] / totalFiles);
								SwingUtilities.invokeLater(() -> progressBar.setValue(progress));
							} catch (IOException ex) {
								throw new UncheckedIOException(ex);
							}
						});
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage());
			}
		});
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new CodeCollectorUI().setVisible(true);
		});
	}
}