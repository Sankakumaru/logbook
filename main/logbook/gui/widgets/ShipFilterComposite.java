package logbook.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logbook.config.ShipGroupConfig;
import logbook.config.bean.ShipGroupBean;
import logbook.constants.AppConstants;
import logbook.data.context.GlobalContext;
import logbook.dto.ItemDto;
import logbook.dto.ShipFilterDto;
import logbook.gui.ShipTable;
import logbook.gui.WindowBase;
import logbook.gui.logic.LayoutLogic;
import logbook.gui.logic.ShipGroupListener;
import logbook.gui.logic.ShipGroupObserver;
import logbook.internal.ShipStyle;
import logbook.util.SwtUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * 所有艦娘一覧で使用するフィルターダイアログ
 * 
 */
public final class ShipFilterComposite extends Composite {

    private final ShipTable shipTable;

    /** 変更イベントを発生させるか？ */
    private boolean changeEnabled = false;
    private boolean panelVisible = true;
    private int currentSelection;

    private Composite contentCompo;

    private Menu switchMenu;
    private MenuItem[] switchMenuItem;

    private Composite groupCompo;
    private Button groupAllButton;
    private final List<Button> groupButtons = new ArrayList<>();
    private ShipGroupBean selectedGroup;

    private Composite typeCompo;
    private Composite typeCheckCompo;
    private final Map<Integer, Button> typeButtons = new TreeMap<>();
    private int maxTypeId;

    private Composite etcCompo;
    /** 名前 */
    private Combo nametext;
    /** 名前.正規表現を使用する */
    private Button regexp;

    /** 全て選択 */
    private Button selectall;

    /** 鍵付き */
    private Button lockedAny;
    /** 鍵付き */
    private Button lockedOnly;
    /** 鍵付きではない */
    private Button lockedNo;
    /** 艦隊に所属 */
    private Button onlyOnFleet;
    /** 遠征中 */
    public Button exceptOnMission;
    /** 要修理 */
    public Button needBath;

    /**
     * Create the dialog.
     * 
     * @param parent シェル
     * @param shipTable 呼び出し元
     * @param filter 初期値
     */
    public ShipFilterComposite(ShipTable shipTable) {
        super(shipTable.getShell(), SWT.NONE);
        this.shipTable = shipTable;
        this.createContents();
    }

    /**
     * Create contents of the dialog.
     */
    private void createContents() {
        // ただ反映するだけ
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (ShipFilterComposite.this.changeEnabled)
                    ShipFilterComposite.this.shipTable.updateFilter(ShipFilterComposite.this.createFilter());
            }
        };
        SelectionListener groupListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ShipFilterComposite.this.groupButtonSelected((Button) e.getSource(), null);
            }
        };

        this.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        this.setLayout(SwtUtils.makeGridLayout(2, 0, 0, 0, 0));

        this.contentCompo = new Composite(this, SWT.NONE);
        this.contentCompo.setLayout(SwtUtils.makeGridLayout(1, 0, 0, 0, 0));
        this.contentCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        // グループタブ
        this.groupCompo = new Composite(this.contentCompo, SWT.NONE);
        this.groupCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        this.groupCompo.setLayout(new RowLayout(SWT.HORIZONTAL));

        this.groupAllButton = new Button(this.groupCompo, SWT.RADIO);
        this.groupAllButton.setText("すべて");
        this.groupAllButton.addSelectionListener(groupListener);

        // 艦種タブ
        this.typeCompo = new Composite(this.contentCompo, SWT.NONE);
        this.typeCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        this.typeCompo.setLayout(SwtUtils.makeGridLayout(1, 0, 0, 0, 0));

        this.selectall = new Button(this.typeCompo, SWT.CHECK);
        this.selectall.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
        this.selectall.setText("全て選択");
        this.selectall.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean select = ShipFilterComposite.this.selectall.getSelection();
                for (Button button : ShipFilterComposite.this.typeButtons.values()) {
                    button.setSelection(select);
                }
                if (ShipFilterComposite.this.changeEnabled)
                    ShipFilterComposite.this.shipTable.updateFilter(ShipFilterComposite.this.createFilter());
            }
        });

        // 艦種カテゴリボタン
        this.typeCheckCompo = new Composite(this.typeCompo, SWT.NONE);
        this.typeCheckCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        this.typeCheckCompo.setLayout(new RowLayout(SWT.HORIZONTAL));

        Composite typeSelectorCompo = new Composite(this.typeCompo, SWT.NONE);
        typeSelectorCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        typeSelectorCompo.setLayout(new RowLayout(SWT.HORIZONTAL));

        SelectionListener categoryListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ShipFilterComposite.this.categoryButtonSelected((Button) e.getSource());
            }
        };
        String[] categoryNames = AppConstants.SHIP_CATEGORY_NAMES;
        int[][] categoryTypes = AppConstants.SHIP_CATEGORY_TYPES;
        for (int i = 0; i < categoryNames.length; ++i) {
            Button button = new Button(typeSelectorCompo, SWT.NONE);
            button.setText(categoryNames[i]);
            button.setData(categoryTypes[i]);
            button.addSelectionListener(categoryListener);
        }

        ///////////////////////////////////

        // その他タブ
        this.etcCompo = new Composite(this.contentCompo, SWT.NONE);
        this.etcCompo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        this.etcCompo.setLayout(SwtUtils.makeGridLayout(2, 0, 0, 0, 0));

        Composite etcgroup = new Composite(this.etcCompo, SWT.NONE);
        etcgroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        etcgroup.setLayout(SwtUtils.makeGridLayout(3, 0, 0, 0, 0));

        this.lockedAny = new Button(etcgroup, SWT.RADIO);
        this.lockedAny.setText("すべて");
        this.lockedAny.addSelectionListener(listener);

        this.lockedOnly = new Button(etcgroup, SWT.RADIO);
        this.lockedOnly.setText("鍵付き");
        this.lockedOnly.addSelectionListener(listener);

        this.lockedNo = new Button(etcgroup, SWT.RADIO);
        this.lockedNo.setText("鍵付きではない");
        this.lockedNo.addSelectionListener(listener);

        this.onlyOnFleet = new Button(etcgroup, SWT.CHECK);
        this.onlyOnFleet.setText("艦隊に所属");
        this.onlyOnFleet.setSelection(false);
        this.onlyOnFleet.addSelectionListener(listener);

        this.exceptOnMission = new Button(etcgroup, SWT.CHECK);
        this.exceptOnMission.setText("遠征中を除外");
        this.exceptOnMission.setSelection(false);
        this.exceptOnMission.addSelectionListener(listener);

        this.needBath = new Button(etcgroup, SWT.CHECK);
        this.needBath.setText("お風呂に入りたい艦娘");
        this.needBath.setSelection(false);
        this.needBath.addSelectionListener(listener);

        //-----------　フリーワード
        Composite namegroup = new Composite(this.etcCompo, SWT.NONE);
        namegroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        namegroup.setLayout(new RowLayout(SWT.HORIZONTAL));

        this.nametext = new Combo(namegroup, SWT.BORDER);
        this.nametext.setLayoutData(new RowData(180, SWT.DEFAULT));
        this.nametext.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (ShipFilterComposite.this.changeEnabled)
                    ShipFilterComposite.this.shipTable.updateFilter(ShipFilterComposite.this.createFilter());
            }
        });
        this.nametext.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // 装備から選択された場合は正規表現をオフ
                ShipFilterComposite.this.regexp.setSelection(false);
            }
        });
        this.nametext.setToolTipText("フリーワード検索(半角SPでAND検索)");

        this.regexp = new Button(namegroup, SWT.CHECK);
        this.regexp.setText("正規表現");
        this.regexp.addSelectionListener(listener);

        //------------------

        Composite subCompo = new Composite(this, SWT.NONE);
        RowLayout subCompoLayout = new RowLayout(SWT.HORIZONTAL);
        subCompoLayout.wrap = false;
        subCompoLayout.spacing = 1;
        subCompoLayout.marginBottom = subCompoLayout.marginRight = subCompoLayout.marginTop = 0;
        subCompo.setLayout(subCompoLayout);
        subCompo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));

        SelectionListener arrowButtonListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = (Button) e.getSource();
                ShipFilterComposite.this.setSelection(
                        (ShipFilterComposite.this.currentSelection + (Integer) button.getData()) % 3);
                ShipFilterComposite.this.shipTable.getShell().layout();
            }
        };
        Button btnLeft = new Button(subCompo, SWT.NONE);
        btnLeft.addSelectionListener(arrowButtonListener);
        btnLeft.setData(2);
        SwtUtils.setButtonImage(btnLeft, SWTResourceManager.getImage(WindowBase.class, AppConstants.R_ICON_LEFT));
        btnLeft.setLayoutData(new RowData(18, 18));

        Button btnRight = new Button(subCompo, SWT.NONE);
        btnRight.addSelectionListener(arrowButtonListener);
        btnRight.setData(1);
        SwtUtils.setButtonImage(btnRight, SWTResourceManager.getImage(WindowBase.class, AppConstants.R_ICON_RIGHT));
        btnRight.setLayoutData(new RowData(18, 18));

        //------------------

        this.switchMenu = new Menu(this);
        final MenuItem[] switchMenuItem = this.switchMenuItem = new MenuItem[3];
        String[] menuText = new String[] { "グループ", "艦種", "その他" };
        for (int i = 0; i < 3; ++i) {
            final int id = i;
            final MenuItem item = switchMenuItem[i] = new MenuItem(this.switchMenu, SWT.CHECK);
            item.setText(menuText[i]);
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    ShipFilterComposite.this.setSelection(id);
                    ShipFilterComposite.this.shipTable.getShell().layout();
                }
            });
        }
        setMenu(this, this.switchMenu);
        this.setData("disable-window-menu", new Object());

        this.switchPanel(0);

        final ShipGroupListener shipGroupListner = new ShipGroupListener() {
            @Override
            public void listChanged() {
                ShipFilterComposite.this.setRedraw(false);
                ShipFilterComposite.this.recreateGroupButtons();
                ShipFilterComposite.this.groupCompo.layout();
                ShipFilterComposite.this.setRedraw(true);
            }

            @Override
            public void groupNameChanged(ShipGroupBean group) {
                int idx = ShipGroupConfig.get().getGroup().indexOf(group);
                if (idx != -1) {
                    ShipFilterComposite.this.groupButtons.get(idx).setText(group.getName());
                    ShipFilterComposite.this.groupCompo.layout();
                }
            }

            /* (非 Javadoc)
             * @see logbook.gui.logic.ShipGroupListener#groupShipChanged(logbook.config.bean.ShipGroupBean)
             */
            @Override
            public void groupShipChanged(ShipGroupBean group) {
                // TODO 自動生成されたメソッド・スタブ

            }
        };
        ShipGroupObserver.addListener(shipGroupListner);
        this.addListener(SWT.Dispose, new Listener() {
            @Override
            public void handleEvent(Event event) {
                ShipGroupObserver.removeListener(shipGroupListner);
            }
        });
    }

    /**
     * 現在のデータでパネル表示内容を更新
     * @param filter
     */
    public void updateContents(ShipFilterDto filter, boolean panelVisible) {
        this.changeEnabled = false;
        Set<String> items = new TreeSet<String>();
        for (ItemDto entry : GlobalContext.getItemMap().values()) {
            items.add(entry.getName());
        }
        this.nametext.remove(0, this.nametext.getItemCount() - 1);
        for (String name : items) {
            this.nametext.add(name);
        }
        this.recreateGroupButtons();
        this.recreateShipTypeButtonos();
        this.applyFilter(filter);
        this.setPanelVisible(panelVisible);
        this.changeEnabled = true;
    }

    private Composite[] getPanels() {
        return new Composite[] { this.groupCompo, this.typeCompo, this.etcCompo };
    }

    private void switchPanel(int panel) {
        Composite[] panels = this.getPanels();
        for (int i = 0; i < 3; ++i) {
            LayoutLogic.hide(panels[i], i != panel);
        }
    }

    public int getSelection() {
        return this.currentSelection;
    }

    public void setSelection(int panel) {
        if ((panel < 0) || (panel >= 3)) {
            throw new IllegalArgumentException("Panel IDが不正です");
        }
        if (this.currentSelection != panel) {
            for (int i = 0; i < 3; ++i) {
                this.switchMenuItem[i].setSelection(panel == i);
            }
            if (this.panelVisible) {
                this.switchPanel(panel);
                this.contentCompo.layout();
            }
            this.currentSelection = panel;
            if (this.changeEnabled) {
                this.shipTable.updateFilter(this.createFilter());
            }
        }
    }

    public boolean getPanelVisible() {
        return this.panelVisible;
    }

    public void setPanelVisible(boolean visible) {
        if (this.panelVisible != visible) {
            LayoutLogic.hide(this, !visible);
            if (visible) {
                this.switchPanel(this.currentSelection);
            }
            this.panelVisible = visible;
        }
    }

    private static void setMenu(Control c, Menu ma) {
        if (c instanceof Composite) {
            for (final Control cc : ((Composite) c).getChildren()) {
                setMenu(cc, ma);
            }
        }
        c.setMenu(ma);
    }

    /**
     * 艦種カテゴリボタンが押された
     */
    private void categoryButtonSelected(Button source) {
        this.typeCompo.setRedraw(false);
        // まずはすべてオフ
        for (Button check : this.typeButtons.values()) {
            check.setSelection(false);
        }
        this.selectall.setSelection(false);

        // 指定されたものだけオン
        int[] types = (int[]) source.getData();
        for (int type : types) {
            Button button = this.typeButtons.get(type);
            if (button != null) {
                button.setSelection(true);
            }
        }
        this.typeCompo.setRedraw(true);

        if (this.changeEnabled)
            this.shipTable.updateFilter(this.createFilter());
    }

    /**
     * グループボタンを削除して再作成
     */
    private void recreateGroupButtons() {
        for (Button button : this.groupButtons) {
            button.setMenu(null);
            button.dispose();
        }
        this.groupButtons.clear();
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = (Button) e.getSource();
                if (button.getSelection()) {
                    ShipFilterComposite.this.groupButtonSelected(button, button.getData());
                }
            }
        };
        for (ShipGroupBean group : ShipGroupConfig.get().getGroup()) {
            Button button = new Button(this.groupCompo, SWT.RADIO);
            button.setText(group.getName());
            button.setData(group);
            button.addSelectionListener(listener);
            button.setMenu(this.switchMenu);
            this.groupButtons.add(button);
        }
        this.groupCompo.layout();
    }

    /**
     * 艦種ボタンを削除して再作成
     */
    private void recreateShipTypeButtonos() {
        for (Button button : this.typeButtons.values()) {
            button.setMenu(null);
            button.dispose();
        }
        this.typeButtons.clear();
        SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button button = (Button) e.getSource();
                if (button.getSelection() == false) {
                    // 艦種のどれかがOFFになったらオフにする
                    ShipFilterComposite.this.selectall.setSelection(false);
                }
                if (ShipFilterComposite.this.changeEnabled)
                    ShipFilterComposite.this.shipTable.updateFilter(ShipFilterComposite.this.createFilter());
            }
        };
        for (Map.Entry<Integer, String> entry : ShipStyle.getMap().entrySet()) {
            String name = entry.getValue();
            int key = entry.getKey();
            if (AppConstants.SHIP_TYPE_INFO.containsKey(key)) {
                name = AppConstants.SHIP_TYPE_INFO.get(key);
                if (name.equals("#")) {
                    // この艦種は表示しない
                    continue;
                }
            }
            Button button = new Button(this.typeCheckCompo, SWT.CHECK);
            button.setText(name);
            button.setData(key);
            button.setSelection(true);
            button.addSelectionListener(listener);
            button.setMenu(this.switchMenu);
            this.typeButtons.put(key, button);
            this.maxTypeId = Math.max(key, this.maxTypeId);
        }
        this.typeCheckCompo.layout();
    }

    /**
     * フィルタデータをパネルに反映
     * グループや艦種などが作られている必要がある
     * @param filter
     */
    private void applyFilter(ShipFilterDto filter) {
        // 選択状態を初期化
        this.groupAllButton.setSelection(false);
        for (Button button : this.groupButtons) {
            button.setSelection(false);
        }
        for (Button button : this.typeButtons.values()) {
            button.setSelection(true);
        }
        this.lockedNo.setSelection(false);
        this.lockedOnly.setSelection(false);
        this.lockedAny.setSelection(false);

        // 名前
        if (!StringUtils.isEmpty(filter.nametext)) {
            this.nametext.setText(filter.nametext);
        }
        // 名前.正規表現を使用する
        this.regexp.setSelection(filter.regexp);

        // 艦種設定
        boolean allselected = true;
        if (filter.enabledType != null) {
            for (int i = 0; i < filter.enabledType.length; ++i) {
                if (this.typeButtons.containsKey(i)) {
                    if (filter.enabledType[i] == false) {
                        allselected = false;
                    }
                    this.typeButtons.get(i).setSelection(filter.enabledType[i]);
                }
            }
        }
        this.selectall.setSelection(allselected);

        // グループ
        Button selectedGroupButton = this.groupAllButton;
        if (filter.group != null) {
            int idx = ShipGroupConfig.get().getGroup().indexOf(filter.group);
            if (idx != -1) {
                selectedGroupButton = this.groupButtons.get(idx);
            }
            this.selectedGroup = filter.group;
        }
        selectedGroupButton.setSelection(true);

        // 鍵付き？
        if (filter.locked == false) {
            this.lockedNo.setSelection(true);
        }
        else if (filter.notlocked == false) {
            this.lockedOnly.setSelection(true);
        }
        else {
            this.lockedAny.setSelection(true);
        }
        // 艦隊に所属
        this.onlyOnFleet.setSelection(!filter.notonfleet);
        // 遠征中を除外
        this.exceptOnMission.setSelection(!filter.mission);
        // お風呂に入りたい
        this.needBath.setSelection(!filter.notneedbath);

        // タブ選択
        this.setSelection(filter.mode);
    }

    public Combo getSearchCombo() {
        return this.nametext;
    }

    private void groupButtonSelected(Button button, Object data) {
        // ラジオボタンはOFFになった時もSelectedが呼ばれるのでONになったものだけ処理する
        if (this.changeEnabled && button.getSelection()) {
            this.selectedGroup = null;
            if (button != this.groupAllButton) {
                this.selectedGroup = (ShipGroupBean) data;
            }
            this.shipTable.updateFilter(this.createFilter());
        }
    }

    /**
     * フィルターを構成する
     * 
     * @return フィルター
     */
    private ShipFilterDto createFilter() {
        ShipFilterDto filter = this.shipTable.getFilter();
        filter.nametext = this.nametext.getText();
        filter.regexp = this.regexp.getSelection();

        filter.enabledType = new boolean[this.maxTypeId + 1];
        for (Button button : this.typeButtons.values()) {
            Integer id = (Integer) button.getData();
            filter.enabledType[id] = button.getSelection();
        }

        filter.group = this.selectedGroup;

        if (this.lockedAny.getSelection()) {
            filter.locked = filter.notlocked = true;
        }
        else if (this.lockedOnly.getSelection()) {
            filter.locked = true;
            filter.notlocked = false;
        }
        else {
            filter.locked = false;
            filter.notlocked = true;
        }
        filter.onfleet = true;
        filter.notonfleet = !this.onlyOnFleet.getSelection();
        filter.mission = !this.exceptOnMission.getSelection();
        filter.notmission = true;
        filter.needbath = true;
        filter.notneedbath = !this.needBath.getSelection();
        filter.mode = this.currentSelection;

        return filter;
    }
}