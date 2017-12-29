package forge.toolbox;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.FileUtil;
import forge.util.Utils;

public class FFileChooser extends FDialog {
    private static final float BACK_ICON_THICKNESS = Utils.scale(2);

    public enum ChoiceType {
        OpenFile,
        SaveFile,
        GetDirectory
    }

    public static void show(String title0, ChoiceType choiceType0, Callback<String> callback0) {
        show(title0, choiceType0, "", "", callback0);
    }
    public static void show(String title0, ChoiceType choiceType0, String defaultFilename0, Callback<String> callback0) {
        show(title0, choiceType0, defaultFilename0, "", callback0);
    }
    public static void show(String title0, ChoiceType choiceType0, String defaultFilename0, String baseDir0, Callback<String> callback0) {
        FFileChooser dialog = new FFileChooser(title0, choiceType0, defaultFilename0, baseDir0, callback0);
        dialog.show();
    }

    private final ChoiceType choiceType;
    private final String baseDir;
    private final Callback<String> callback;

    private final FList<File> lstFiles   = add(new FileList());
    private final FTextField txtFilename = add(new FilenameField());

    private FFileChooser(String title0, ChoiceType choiceType0, String defaultFilename0, String baseDir0, Callback<String> callback0) {
        super(title0, 3);
        choiceType = choiceType0;
        if (choiceType == ChoiceType.GetDirectory) {
            if (defaultFilename0.endsWith(File.separator)) { //if getting directory, don't end with a slash
                defaultFilename0 = defaultFilename0.substring(0, defaultFilename0.length() - 1);
            }
        }
        txtFilename.setFont(FSkinFont.get(12));
        txtFilename.setText(defaultFilename0);
        txtFilename.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                refreshFileList();
            }
        });

        initButton(0, "OK", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                activateSelectedFile(true);
            }
        });
        initButton(1, "New Folder", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                final File dir = getCurrentDir();
                if (dir == null) {
                    FOptionPane.showErrorDialog("Cannot add new folder to invalid folder.", "Invalid Folder");
                    return;
                }

                FOptionPane.showInputDialog("Enter name for new folder", new Callback<String>() {
                    @Override
                    public void run(String result) {
                        if (StringUtils.isEmpty(result)) { return; }

                        try {
                            File newDir = new File(dir, result);
                            if (newDir.mkdirs()) {
                                txtFilename.setText(newDir.getAbsolutePath());
                                refreshFileList();
                                return;
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        FOptionPane.showErrorDialog("\"" + result + "\" is not a valid folder name.", "Invalid Name");
                    }
                });
            }
        });
        initButton(2, "Cancel", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                hide();
            }
        });

        baseDir = baseDir0;
        callback = callback0;
        refreshFileList();
    }

    @Override
    protected float layoutAndGetHeight(float width, float maxHeight) {
        float padding = FOptionPane.PADDING;
        float w = width - 2 * padding;

        float fieldHeight = txtFilename.getHeight();
        float listHeight = maxHeight - fieldHeight - 2 * padding;
        float x = padding;
        float y = padding;
        txtFilename.setBounds(x, y, w, fieldHeight);
        y += fieldHeight + padding;
        lstFiles.setBounds(x, y, w, listHeight);
        return maxHeight;
    }

    private File getCurrentDir() {
        String filename = getSelectedFilename();
        File dir = new File(filename);
        if (!dir.exists() || !dir.isDirectory()) {
            int idx = filename.lastIndexOf(File.separatorChar);
            if (idx != -1) {
                dir = new File(filename.substring(0, idx));
            }
        }
        if (dir.exists() && dir.isDirectory()) {
            if (choiceType == ChoiceType.GetDirectory) {
                dir = dir.getParentFile(); //show parent folder's files and select folder
            }
            return dir;
        }
        return null;
    }

    private void refreshFileList() {
        File dir = getCurrentDir();
        if (dir != null) {
            setFileListForDir(dir);
        }
        else {
            lstFiles.setListData(File.listRoots());
        }
        scrollSelectionIntoView();
    }

    private void setFileListForDir(File dir) {
        FilenameFilter filter = null;
        if (choiceType == ChoiceType.GetDirectory) {
            //don't list files if getting directory
            filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            };
        }
        lstFiles.setListData(dir.listFiles(filter));
    }

    private void scrollSelectionIntoView() {
        String filename = getSelectedFilename();
        for (int i = 0; i < lstFiles.getCount(); i++) {
            if (lstFiles.getItemAt(i).getAbsolutePath().equals(filename)) {
                lstFiles.scrollIntoView(i);
                return;
            }
        }
    }

    private String getSelectedFilename() {
        return baseDir + txtFilename.getText();
    }

    private void activateSelectedFile(boolean fromOkButton) {
        String filename = getSelectedFilename();
        File file = new File(filename);
        boolean returnDirectory = (choiceType == ChoiceType.GetDirectory);
        if (file.exists() && file.isDirectory() && (!fromOkButton || !returnDirectory)) {
            //if directory activated not from OK button, open it within dialog
            setFileListForDir(file);
            if (lstFiles.getCount() > 0) { //select first item if any
                txtFilename.setText(lstFiles.getItemAt(0).getAbsolutePath());
            }
            else {
                txtFilename.setText(file.getAbsolutePath() + File.separator); //indicate no selection in directory
            }
            scrollSelectionIntoView();
            return;
        }

        //validate return value
        if (returnDirectory) {
            if (!file.exists() || !file.isDirectory()) {
                FOptionPane.showErrorDialog("No folder exists with the selected path.", "Invalid Folder");
                return;
            }
        }
        else {
            if ((!file.exists() && choiceType == ChoiceType.OpenFile) || file.isDirectory()) {
                FOptionPane.showErrorDialog("No file exists with the selected path.", "Invalid File");
                return;
            }
        }

        hide();
        if (returnDirectory) {
            filename += File.separator; //re-append separator if returning directory
        }
        callback.run(filename);
    }

    private void back() {
        int idx = txtFilename.getText().lastIndexOf(File.separatorChar);
        if (idx != -1) {
            txtFilename.setText(txtFilename.getText().substring(0, idx));
            refreshFileList();
        }
    }

    private void renameFile(final File file) {
        final File dir = file.getParentFile();
        if (dir == null) {
            FOptionPane.showErrorDialog("Cannot rename file in invalid folder.", "Invalid Folder");
            return;
        }

        String title;
        if (file.isDirectory()) {
            title = "Enter new name for folder";
        }
        else {
            title = "Enter new name for file";
        }
        FOptionPane.showInputDialog(title, file.getName(), new Callback<String>() {
            @Override
            public void run(String result) {
                if (StringUtils.isEmpty(result)) { return; }

                try {
                    File newFile = new File(dir, result);
                    if (file.renameTo(newFile)) {
                        txtFilename.setText(newFile.getAbsolutePath());
                        refreshFileList();
                        return;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                FOptionPane.showErrorDialog("\"" + result + "\" is not a valid name.", "Invalid Name");
            }
        });
    }

    private void deleteFile(final Integer index, final File file) {
        FOptionPane.showConfirmDialog("Are you sure you wish to proceed with delete? This action cannot be undone.",
                "Delete " + (file.isDirectory() ? "Folder" : "File"), "Delete", "Cancel", new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    try {
                        if (FileUtil.deleteDirectory(file)) { //this will ensure directory or file deleted
                            lstFiles.removeItem(file);
                            if (lstFiles.getCount() > index) { //select next item if possible
                                txtFilename.setText(lstFiles.getItemAt(index).getAbsolutePath());
                            }
                            else if (lstFiles.getCount() > 0) { //select new last item otherwise
                                txtFilename.setText(lstFiles.getItemAt(lstFiles.getCount() - 1).getAbsolutePath());
                            }
                            else {
                                File dir = getCurrentDir();
                                if (dir != null) {
                                    txtFilename.setText(dir.getAbsolutePath() + File.separator); //indicate no selection in directory
                                }
                            }
                            return;
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    FOptionPane.showErrorDialog("Could not delete file.");
                }
            }
        });
    }

    private class FilenameField extends FTextField {
        @Override
        public boolean tap(float x, float y, int count) {
            if (x < getLeftPadding()) { //handle tapping on back icon
                back();
                return true;
            }
            return super.tap(x, y, count);
        }

        @Override
        protected float getLeftPadding() {
            return getHeight();
        }

        @Override
        public void draw(Graphics g) {
            super.draw(g);

            //draw back icon
            float w = getHeight();
            float h = w;
            float x = w * 0.35f; 
            float y = h / 2;
            float offsetX = w / 8;
            float offsetY = h / 6;

            g.drawLine(BACK_ICON_THICKNESS, FORE_COLOR, x + offsetX, y - offsetY, x - offsetX, y);
            g.drawLine(BACK_ICON_THICKNESS, FORE_COLOR, x - offsetX, y, x + offsetX, y + offsetY);

            x += w * 0.3f;

            g.drawLine(BACK_ICON_THICKNESS, FORE_COLOR, x + offsetX, y - offsetY, x - offsetX, y);
            g.drawLine(BACK_ICON_THICKNESS, FORE_COLOR, x - offsetX, y, x + offsetX, y + offsetY);
        }
    }

    private class FileList extends FList<File> {
        private FileList() {
            setListItemRenderer(new ListItemRenderer<File>() {
                private int prevTapIndex = -1;

                @Override
                public float getItemHeight() {
                    return FChoiceList.DEFAULT_ITEM_HEIGHT;
                }

                @Override
                public boolean tap(Integer index, File value, float x, float y, int count) {
                    if (count == 2 && index == prevTapIndex) {
                        activateSelectedFile(false);
                        return true;
                    }

                    txtFilename.setText(value.getAbsolutePath());
                    prevTapIndex = index;

                    if (value.isDirectory() && x < getItemHeight() + 2 * FList.PADDING) {
                        activateSelectedFile(false); //open folders if icon single tapped
                    }
                    return true;
                }

                @Override
                public boolean showMenu(final Integer index, final File value, FDisplayObject owner, float x, float y) {
                    txtFilename.setText(value.getAbsolutePath());
                    FPopupMenu menu = new FPopupMenu() {
                        @Override
                        protected void buildMenu() {
                            String suffix = value.isDirectory() ? " Folder" : " File";
                            addItem(new FMenuItem("Rename" + suffix, FSkinImage.EDIT,
                                    new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    renameFile(value);
                                }
                            }));
                            addItem(new FMenuItem("Delete" + suffix, FSkinImage.DELETE,
                                    new FEventHandler() {
                                @Override
                                public void handleEvent(FEvent e) {
                                    deleteFile(index, value);
                                }
                            }));
                        }
                    };

                    menu.show(owner, x, y);
                    return true;
                }

                @Override
                public void drawValue(Graphics g, Integer index, File value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    if (value.isDirectory()) {
                        float iconSize = h;
                        g.drawImage(FSkinImage.FOLDER, x, y + (h - iconSize) / 2, iconSize, iconSize);
                        x += iconSize + FList.PADDING; 
                    }
                    g.drawText(value.getName(), font, foreColor, x, y, w, h, false, HAlignment.LEFT, true);
                }
            });
        }

        @Override
        protected void drawBackground(Graphics g) {
            //draw no background
        }

        @Override
        public void drawOverlay(Graphics g) {
            super.drawOverlay(g);
            g.drawRect(1.5f, FChoiceList.BORDER_COLOR, 0, 0, getWidth(), getHeight());
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (getItemAt(index).getAbsolutePath().equals(getSelectedFilename())) {
                return FChoiceList.SEL_COLOR; //don't show SEL_COLOR if in multi-select mode
            }
            if (index % 2 == 1) {
                return FChoiceList.ALT_ITEM_COLOR;
            }
            return FChoiceList.ITEM_COLOR;
        }

        @Override
        protected boolean drawLineSeparators() {
            return false;
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        switch (keyCode) {
        case Keys.ENTER:
            activateSelectedFile(false);
            return true;
        case Keys.ESCAPE:
            if (Forge.endKeyInput()) { return true; }
            break; //let FDialog handle it
        case Keys.BACK:
        case Keys.BACKSPACE:
            back();
            return true;
        }
        return super.keyDown(keyCode);
    }
}
