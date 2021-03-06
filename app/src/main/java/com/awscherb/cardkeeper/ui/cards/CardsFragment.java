package com.awscherb.cardkeeper.ui.cards;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.awscherb.cardkeeper.R;
import com.awscherb.cardkeeper.dagger.component.DaggerCardsComponent;
import com.awscherb.cardkeeper.dagger.module.CardsPresenterModule;
import com.awscherb.cardkeeper.data.model.ScannedCode;
import com.awscherb.cardkeeper.ui.base.BaseApplication;
import com.awscherb.cardkeeper.ui.base.BaseFragment;
import com.awscherb.cardkeeper.ui.card_detail.CardDetailActivity;
import com.awscherb.cardkeeper.ui.listener.RecyclerItemClickListener;
import com.awscherb.cardkeeper.ui.scan.ScanActivity;
import com.awscherb.cardkeeper.ui.scan.ScanFragment;
import com.google.zxing.BarcodeFormat;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;

public class CardsFragment extends BaseFragment implements CardsContract.View {

    public static final int REQUEST_GET_CODE = 3;

    @Inject CardsContract.Presenter presenter;

    @BindView(R.id.fragment_cards_recycler) RecyclerView recyclerView;
    @BindView(R.id.fragment_cards_fab) FloatingActionButton fab;

    LinearLayoutManager layoutManager;
    CardsAdapter scannedCodeAdapter;

    //================================================================================
    // New Instance
    //================================================================================

    public static CardsFragment newInstance() {
        return new CardsFragment();
    }

    //================================================================================
    // Lifecycle methods
    //================================================================================

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DaggerCardsComponent.builder()
                .cardsPresenterModule(new CardsPresenterModule(this))
                .servicesComponent(((BaseApplication) getActivity().getApplication()).getServicesComponent())
                .build().inject(this);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cards, container, false);
        ButterKnife.bind(this, v);

        layoutManager = new LinearLayoutManager(getActivity());
        scannedCodeAdapter = new CardsAdapter(getActivity(), presenter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(scannedCodeAdapter);

        setupListeners();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.loadCards();
    }

    @Override
    public void onPause() {
        presenter.onViewDestroyed();
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GET_CODE && resultCode == RESULT_OK) {
            ScannedCode scannedCode = new ScannedCode();
            scannedCode.setId(System.currentTimeMillis());
            scannedCode.setText(data.getStringExtra(ScanFragment.EXTRA_BARCODE_TEXT));
            scannedCode.setFormat((BarcodeFormat) data.getSerializableExtra(ScanFragment.EXTRA_BARCODE_FORMAT));

            EditText input = new EditText(getActivity());
            input.setHint(R.string.dialog_card_name_hint);
            input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_name)
                    .setView(input)
                    .setPositiveButton(R.string.action_add,
                            (dialog, which) -> {
                                scannedCode.setTitle(input.getText().toString());
                                presenter.addNewCard(scannedCode);
                            })
                    .setNegativeButton(R.string.action_cancel,
                            (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    //================================================================================
    // View methods
    //================================================================================

    @Override
    public void showCards(List<ScannedCode> codes) {
        scannedCodeAdapter.swapObjects(codes);
    }

    @Override
    public void onCardAdded(ScannedCode code) {
        Snackbar.make(getView(), getString(R.string.fragment_cards_added_card, code.getTitle()), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onCardDeleted() {
        Snackbar.make(getView(), R.string.fragment_cards_deleted_card, Snackbar.LENGTH_SHORT).show();
    }

    //================================================================================
    // Helper methods
    //================================================================================

    private void setupListeners() {
        fab.setOnClickListener(v1 -> startActivityForResult(
                new Intent(getActivity(), ScanActivity.class), REQUEST_GET_CODE));

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),
                (view, position) -> {
                    Intent i = new Intent(getActivity(), CardDetailActivity.class);
                    i.putExtra(CardDetailActivity.EXTRA_CARD_ID,
                            scannedCodeAdapter.getItem(position).getId());
                    startActivity(i);
                }));
    }
}
