package ml.qingsu.fuckview.ui.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.AutoTransition;
import android.transition.Explode;
import android.transition.Fade;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import ml.qingsu.fuckview.Constant;
import ml.qingsu.fuckview.R;
import ml.qingsu.fuckview.implement.Searchable;
import ml.qingsu.fuckview.models.BlockModel;
import ml.qingsu.fuckview.ui.activities.MainActivity;
import ml.qingsu.fuckview.ui.fragments.select_app.SelectAppWizard;

/**
 * By w568w on 2017-7-6.
 */

public class MainFragment extends Fragment implements Searchable {
    private AppAdapter adapter;
    private Activity context;
    PackageManager pm;
    ArrayList<BlockModel> models;
    ListView listView;
    String searchText = "";
    private ArrayList<Integer> deleteList = new ArrayList<>();

    public static String getAppTitle(PackageManager pm, String packageName) {
        try {
            return pm.getPackageInfo(packageName, 0).applicationInfo.loadLabel(pm).toString();

        } catch (Exception ignored) {
        }
        return packageName;
    }

    public static Drawable getAppIcon(PackageManager pm, String packageName) {
        try {
            Drawable drawable = pm.getPackageInfo(packageName, 0).applicationInfo.loadIcon(pm);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;

        } catch (Exception ignored) {
        }
        return new BitmapDrawable();
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        context = getActivity();
        pm = context.getPackageManager();
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.main_fragment, null);
        listView = (ListView) layout.findViewById(R.id.listView);
        models = BlockModel.readModel();
        try {
            Collections.sort(models, new Comparator<BlockModel>() {
                @Override
                public int compare(BlockModel blockModel, BlockModel t1) {
                    String s1 = getAppTitle(pm, blockModel.packageName);
                    String s2 = getAppTitle(pm, t1.packageName);
                    //JDK7: RFE: 6804124
                    //Synopsis: Updated sort behavior for Arrays and Collections may throw an IllegalArgumentException
                    if (s1.equals(s2)) {
                        return 0;
                    }
                    return Collator.getInstance(Locale.CHINA).compare(s1, s2);
                }
            });
        } catch (IllegalArgumentException e) {
            //So we will not sort them,OK?
            e.printStackTrace();
        }
        adapter = new AppAdapter();
        listView.setAdapter(adapter);
        registerForContextMenu(listView);
        final FloatingActionButton button = (FloatingActionButton) layout.findViewById(R.id.fab);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectAppWizard tw = new SelectAppWizard();
                Bundle bundle = new Bundle();
                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("system_app", false)) {
                    bundle.putBoolean("sys", true);
                }
                tw.setArguments(bundle);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).setFragment(tw);
                }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BlockModel model = models.get(i);
                Bundle bundle = new Bundle();
                bundle.putString("pkg", model.packageName);
                bundle.putString("record", model.record);
                bundle.putString("className", model.className);
                InfoFragment infoFragment = new InfoFragment();
                infoFragment.setArguments(bundle);
                if (context instanceof MainActivity) {
                    ((MainActivity) context).setFragment(infoFragment);
                }
                ImageView iconView = (ImageView) view.findViewById(R.id.icon);

                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                //TODO Does not work but I don't know how to fix it.
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    //Set animation
//                    infoFragment.setSharedElementEnterTransition(new InfoTransition());
//                    infoFragment.setEnterTransition(new Fade());
//                    setExitTransition(new Fade());
//                    infoFragment.setSharedElementReturnTransition(new InfoTransition());
//
//                    transaction.addSharedElement(iconView, "icon");
//                }

                transaction
                        .replace(R.id.fl, infoFragment)
                        .commit();
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            boolean scrollFlag = false;
            boolean isFirst = true;

            int lastVisibleItemPosition;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                scrollFlag = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING;
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isFirst) {

                    if (firstVisibleItem < lastVisibleItemPosition) {
                        button.show();
                        //执行向上滑动时要做的逻辑
                    } else if (firstVisibleItem > lastVisibleItemPosition) {
                        button.hide();
                    }
                    //记录当前条目
                    lastVisibleItemPosition = firstVisibleItem;

                }
                isFirst = false;

            }
        });
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (context instanceof MainActivity) {
            ((MainActivity) context).shouldShowFAQ = true;
            ((MainActivity) context).currentFragment = this;
            //AppCompatActivity类提供了一个可以在API9上使用的invalidateOptionsMenu方法
            //本来这个方法在Activity类上需要API11的
            //所以需要转型
            //(PS:呆萌的AS告诉我转型是多余的2333)
            ((AppCompatActivity) context).invalidateOptionsMenu();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        BlockModel model = models.get(info.position);
        if (deleteList.size() > 0) {
            menu.add(0, 4, Menu.NONE, R.string.delete_selections);
            menu.add(0, 5, Menu.NONE, R.string.share_selections);
            return;
        }
        menu.add(0, 1, Menu.NONE, R.string.delete_item);
        menu.add(0, 3, Menu.NONE, R.string.share);
        menu.add(0, 6, Menu.NONE, model.enable ? R.string.disable_item : R.string.enable_item);
        menu.add(0, 7, Menu.NONE, R.string.start_app);
    }

    @Override
    public void setSearchText(String text) {
        searchText = text;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final BlockModel model = models.get(menuInfo.position);
        switch (item.getItemId()) {
            case 1:
                models.remove(menuInfo.position);
                adapter.notifyDataSetChanged();
                saveAll();
                break;
            case 2:
                new AlertDialog.Builder(context)
                        .setTitle(R.string.confirm)
                        .setMessage("是否要将此项设为不按类名定位？\n这样可能会解决一些无法屏蔽的问题，但是也会增加一些误伤的几率。\n\n注意:此操作不可逆!")
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                model.className = "*";
                                models.set(menuInfo.position, model);
                                adapter.notifyDataSetChanged();
                                saveAll();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            case 3:
                share(model.toString());
                break;
            case 4:
                ArrayList<BlockModel> arrayList = new ArrayList<>();
                for (Integer postion : deleteList) {
                    arrayList.add(models.get(postion));
                }
                models.removeAll(arrayList);
                deleteList.clear();
                adapter.notifyDataSetChanged();

                saveAll();
                break;
            case 5:
                ArrayList<BlockModel> shares = new ArrayList<>();
                for (Integer postion : deleteList) {
                    shares.add(models.get(postion));
                }
                deleteList.clear();
                StringBuilder stringBuilder = new StringBuilder();
                for (BlockModel model1 : shares) {
                    stringBuilder.append(model1.toString()).append("\n");
                }
                share(stringBuilder.toString());
                adapter.notifyDataSetChanged();
                break;
            case 6:
                model.enable = !model.enable;
                models.set(menuInfo.position, model);
                adapter.notifyDataSetChanged();
                saveAll();
                break;
            case 7:
                try {
                    startActivity(pm.getLaunchIntentForPackage(model.packageName));
                } catch (Exception e) {
                    Toast.makeText(getActivity(), R.string.cant_start_app, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void share(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_share_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAll() {
        MainActivity.writePreferences("", Constant.LIST_NAME);
        for (BlockModel bm : models) {
            bm.save();
        }
    }

    private class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return models.size();
        }

        @Override
        public Object getItem(int i) {
            return models.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private boolean contains(ArrayList<Integer> arrayList, int i) {
            for (Integer integer : arrayList) {
                if (integer == i) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            if (view == null || !(view instanceof ViewGroup)) {
                view = context.getLayoutInflater().inflate(R.layout.main_fragment_list_item, null);
            }
            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            TextView title = (TextView) view.findViewById(R.id.app_name);
            TextView type = (TextView) view.findViewById(R.id.class_name);
            ViewCompat.setTransitionName(icon, String.valueOf(i) + "_image");
            final CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox);
            checkbox.setChecked(contains(deleteList, i));
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isChecked = contains(deleteList, i);
                    if (isChecked) {
                        deleteList.remove(Integer.valueOf(i));
                    } else {
                        deleteList.add(i);
                    }
                    getActivity().setTitle(deleteList.isEmpty() ? R.string.app_name : R.string.multi_select);
                }
            });
            BlockModel bm = models.get(i);
            try {
                icon.setImageDrawable(getAppIcon(pm, bm.packageName));
                title.setText(getAppTitle(pm, bm.packageName));
            } catch (Exception e) {
                //Application not found
                icon.setImageResource(R.drawable.ic_launcher);
                title.setText(bm.packageName);
            }
            //是否是经典模式

            if (bm.text.isEmpty()) {
                type.setText(bm.className);
            } else {
                type.setText(String.format("%s ---> %s", bm.className, bm.text));
            }
            if (!bm.enable) {
                view.setBackgroundColor(Color.GRAY);
            }
            if (!searchText.isEmpty() && !title.getText().toString().toLowerCase().contains(searchText.toLowerCase())) {
                view = new View(context);
                view.setVisibility(View.GONE);
            }
            return view;
        }

    }
}
