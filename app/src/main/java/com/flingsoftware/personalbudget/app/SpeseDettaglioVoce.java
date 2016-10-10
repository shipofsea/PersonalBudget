/*
 * Copyright (c) - Software developed by iClaude.
 */

package com.flingsoftware.personalbudget.app;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.flingsoftware.personalbudget.R;
import com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiPreferenze;
import com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiSuoni;
import com.flingsoftware.personalbudget.app.utility.AvatarImageBehavior;
import com.flingsoftware.personalbudget.customviews.MioToast;
import com.flingsoftware.personalbudget.database.DBCSpeseRipetute;
import com.flingsoftware.personalbudget.database.DBCSpeseSostenute;
import com.flingsoftware.personalbudget.database.DBCSpeseVoci;
import com.flingsoftware.personalbudget.database.FunzioniAggiornamento;
import com.flingsoftware.personalbudget.utilita.BlurBuilder;
import com.flingsoftware.personalbudget.utilita.ListViewIconeVeloce;
import com.flingsoftware.personalbudget.utilita.UtilityVarious;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiDettaglioVoce.OPERAZIONE_ELIMINAZIONE;
import static com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiDettaglioVoce.OPERAZIONE_MODIFICA;
import static com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiDettaglioVoce.TIPO_OPERAZIONE;
import static com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiPreferenze.VALUTA_PRINCIPALE;
import static com.flingsoftware.personalbudget.app.MainPersonalBudget.CostantiVarie.WIDGET_AGGIORNA;
import static com.flingsoftware.personalbudget.app.SpeseEntrateEliminaVociRipetute.ELIMINA_DA_OGGI;
import static com.flingsoftware.personalbudget.app.SpeseEntrateEliminaVociRipetute.ELIMINA_SOLO_QUESTA;
import static com.flingsoftware.personalbudget.app.SpeseEntrateEliminaVociRipetute.ELIMINA_TUTTE;
import static com.flingsoftware.personalbudget.database.StringheSQL.ESTRAI_BUDGET_PER_AGGIUNTA_ELIMINAZIONE_SPESA;
import static com.flingsoftware.personalbudget.database.StringheSQL.ESTRAI_BUDGET_PER_ELIMINAZIONE_SPESE_RIPETUTE;


public class SpeseDettaglioVoce extends AppCompatActivity implements SpeseEntrateEliminaVociRipetute.EliminaVociRipetuteListener {

	//costanti pubbliche
	public interface CostantiPubbliche {
		String VOCE_ID = "id";
		String VOCE_IMPORTO = "importo";
		String VOCE_TAG = "tag";
		String VOCE_DATA = "data";
		String VOCE_DESCRIZIONE = "descrizione";
		String VOCE_RIPETIZIONE_ID = "ripetizione_id";
		String VOCE_IMPORTO_VALPRIN = "importoValprin";
		String VOCE_VALUTA = "valuta";
		String VOCE_CONTO = "conto";
		String VOCE_FAVORITE = "favorite";
	}

	// Private constants.
	private static final float PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.8f;
	private static final float PERCENTAGE_TO_HIDE_TITLE_DETAILS = 0.3f;
	private static final int ALPHA_ANIMATIONS_DURATION = 200;
	private static final String TAG = "SpeseDettaglioVoce";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.spese_entrate_dettaglio_voce);

		// Toolbar per menu opzioni.
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(null);

		// Listener for appbar expanded/collapsed to show the title correctly.
		AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
		appBarLayout.addOnOffsetChangedListener(new AppBarStateChangeListener());

		//ottengo i reference ai vari componenti
		tvVoce = (TextView) findViewById(R.id.tvVoce);
		tvImporto = (TextView) findViewById(R.id.tvImporto);
		tvConto = (TextView) findViewById(R.id.sedv_tvConto);
		tvData = (TextView) findViewById(R.id.tvData);
		tvDescrizione = (TextView) findViewById(R.id.tvDescrizione);
		tvRipetizione = (TextView) findViewById(R.id.dettagli_voce_tvRipetizione);
		tvFineRipetizione = (TextView) findViewById(R.id.dettagli_voce_tvFineRipetizione);
		ivIcona = (ImageView) findViewById(R.id.spese_entrate_dettaglio_voce_ivIcona);
		ivIconToolbar = (ImageView) findViewById(R.id.ivIconToolbar);
		fabAlto = (FloatingActionButton) findViewById(R.id.fab);
		tvToolbarTitle = (TextView) findViewById(R.id.main_textview_title);
		llExpandedTitle = (LinearLayout) findViewById(R.id.main_linearlayout_title);

		// Set custom layout behavior on ImageView ivIcona.
		AvatarImageBehavior avatarImageBehavior = new AvatarImageBehavior(this);
		CoordinatorLayout.LayoutParams params =
				(CoordinatorLayout.LayoutParams) ivIcona.getLayoutParams();
		params.setBehavior(avatarImageBehavior);

		// Set height of icon in the toolbar to 70% of action bar height.
		int ivIconToolbarSize = (int) (UtilityVarious.getActionBarHeight(this) * 0.7);
		ivIconToolbar.getLayoutParams().width = ivIconToolbarSize;
		ivIconToolbar.getLayoutParams().height = ivIconToolbarSize;
		ivIconToolbar.requestLayout();

		//recupero i dettagli della voce passati dall'Activity chiamante
		Bundle extras = getIntent().getExtras();
		id = extras.getLong(CostantiPubbliche.VOCE_ID);
		importo = extras.getDouble(CostantiPubbliche.VOCE_IMPORTO);
		tag = extras.getString(CostantiPubbliche.VOCE_TAG);
		data = extras.getLong(CostantiPubbliche.VOCE_DATA);
		descrizione = extras.getString(CostantiPubbliche.VOCE_DESCRIZIONE);
		ripetizione_id = extras.getLong(CostantiPubbliche.VOCE_RIPETIZIONE_ID);
		importoValprin = extras.getDouble(CostantiPubbliche.VOCE_IMPORTO_VALPRIN);
		valuta = extras.getString(CostantiPubbliche.VOCE_VALUTA);
		conto = extras.getString(CostantiPubbliche.VOCE_CONTO);
		preferito = extras.getInt(CostantiPubbliche.VOCE_FAVORITE);

		//visualizzo i valori recuperati nel layout
		tvVoce.setText(tag);
		tvToolbarTitle.setText(tag);
		new LoadImagesTask().execute(tag);

		//ricavo la valuta di default
		ricavaValuta();

		NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.getDefault());
		nf.setCurrency(currValuta);
		String importoFormattato = nf.format(importoValprin);
		tvImporto.setText(importoFormattato);
		tvConto.setText(conto);
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, miaLocale);
		tvData.setText(df.format(new Date(data)));
		if (descrizione.length() > 0) {
			tvDescrizione.setText(descrizione);
		} else {
			tvDescrizione.setVisibility(View.GONE);
			findViewById(R.id.tvDescrizioneTitolo).setVisibility(View.GONE);
		}
		if (!valuta.equals(currValuta.getCurrencyCode())) {
			NumberFormat nfValuta = NumberFormat.getInstance(Locale.getDefault());
			NumberFormat nfCambio = NumberFormat.getInstance();
			nfCambio.setMaximumFractionDigits(4);

			float cambio = (float) (importoValprin / importo);
			((TextView) findViewById(R.id.tvImportoOriginale)).setText(nfValuta.format(importo) + " " + Currency.getInstance(valuta).getSymbol());
			((TextView) findViewById(R.id.tvTassoCambio)).setText(nfCambio.format(cambio));
		} else {
			findViewById(R.id.spese_entrate_dettaglio_voce_rlImporto).setVisibility(View.GONE);
		}

		//recupero i dettagli delle spese ripetute in un thread separato
		dbcSpeseSostenute = new DBCSpeseSostenute(SpeseDettaglioVoce.this);
		dbcSpeseRipetute = new DBCSpeseRipetute(SpeseDettaglioVoce.this);
		if (ripetizione_id != 1) {
			new ImpostaDettagliSpesaRipetuta().execute(ripetizione_id);
		} else {
			findViewById(R.id.spese_entrate_dettaglio_voce_rlRipetizione).setVisibility(View.GONE);
		}

		new CaricaSuoniTask().execute();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

	/*
    Enter animation. NestedScrollView, which contains a CardView representing the main content,
    is moved upwards with an animation.
 */
	@Override
	public void onEnterAnimationComplete() {
		super.onEnterAnimationComplete();

		View contentView = findViewById(R.id.nsv_main_content);
		float offset = getResources().getDimensionPixelSize(R.dimen.content_offset_y);
		Interpolator interpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in);
		contentView.setVisibility(View.VISIBLE);
		fabAlto.setVisibility(View.VISIBLE);
		contentView.setTranslationY(offset);
		contentView.setAlpha(0.3f);
		contentView.animate()
				.translationY(0f)
				.alpha(1f)
				.setInterpolator(interpolator)
				.setDuration(500L)
				.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_speseentratedettagliovoce, menu);

		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.menu_speseEntrateDettaglioVoce_cancella:
				eliminaVoce();

				return true;
			case R.id.menu_speseEntrateDettaglioVoce_duplica:
				duplicaVoce(); // duplicazione voce

				return true;
			case android.R.id.home:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					finishAfterTransition();
				} else {
					finish();
				}

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (suoniAbilitati && confermaElimina) {
			soundPool.play(mappaSuoni.get(CostantiSuoni.SUONO_CANCELLAZIONE), 1, 1, 1, 0, 1f);
		} else if (suoniAbilitati && confermaDuplica) {
			soundPool.play(mappaSuoni.get(CostantiSuoni.SUONO_AGGIUNGI_SPESA_ENTRATA), 1, 1, 1, 0, 1f);
		}
	}


	/*
	Classe per intercettare quando la Toolbar è collassata o espansa, in modo tale da visualizzare
	il fab per la modifica della voce in basso a destra.
	 */
	private class AppBarStateChangeListener implements AppBarLayout.OnOffsetChangedListener {
		private final static int ESPANSO = 0;
		private final static int COLLASSATO = 1;
		private final static int INTERMEDIO = 2;
		private int statoCorrente = INTERMEDIO;
		private boolean fabBassoVisibile = false;

		@Override
		public final void onOffsetChanged(AppBarLayout appBarLayout, int i) {
			int maxScroll = appBarLayout.getTotalScrollRange();
			float percentage = (float) Math.abs(i) / (float) maxScroll;
			handleToolbarTitleVisibility(percentage);
			handleExpandedTitleVisibility(percentage);
		}
	}


	/*
	 * Ricavo la valuta principale salvata nelle preferenze.
	 */
	private void ricavaValuta() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String codiceValutaDefault = pref.getString(VALUTA_PRINCIPALE, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
		currValuta = Currency.getInstance(codiceValutaDefault);

	}


	// Duplicazione voce: aggiungo una nuova voce con data oggi con gli stessi dati.
	private void duplicaVoce() {
		UtilityVarious.visualizzaDialogOKAnnulla(SpeseDettaglioVoce.this,
				getString(R.string.dettagli_voce_conferma_duplica_titolo),
				getString(R.string.dettagli_voce_conferma_duplica_msg),
				getString(R.string.ok),
				true, getString(R.string.cancella),
				0,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						confermaDuplica = true;
						new DuplicaVoce().execute((Object[]) null);
					}
				});
	}


	// elimina 1 o pi� voci di spesa ripetute
	private void eliminaVoce() {
		if (ripetizione_id == 1) {
			UtilityVarious.visualizzaDialogOKAnnulla(SpeseDettaglioVoce.this,
					getString(R.string.dettagli_voce_conferma_elimina_titolo),
					getString(R.string.dettagli_voce_conferma_elimina_msg),
					getString(R.string.ok),
					true, getString(R.string.cancella),
					0,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							confermaElimina = true;
							new EliminaQuestaSpesaTask().execute(id);
							//aggiorno la tabella spese_budget campo spesa_sost a seguito della eliminazione della/e spesa/e
							new AggiornaTabellaBudgetTask(ESTRAI_BUDGET_PER_AGGIUNTA_ELIMINAZIONE_SPESA, "%" + tag + "%", Long.valueOf(data).toString(), Long.valueOf(data).toString()).execute((Object[]) null);
						}
					});
		} else {
			DialogFragment dialogEliminaVociRipetute = new SpeseEntrateEliminaVociRipetute();
			dialogEliminaVociRipetute.show(getSupportFragmentManager(), "EliminaVociRipetute");
		}
	}


	// Azione associata alla pressione del FAB.
	public void fabPremuto(View v) {
		modificaVoce();
	}

	// modifica questa voce
	private void modificaVoce() {
		Intent modificaSpesa = new Intent(SpeseDettaglioVoce.this, SpeseAggiungi.class);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_ID, id);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_TAG, tag);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_IMPORTO, importo);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_VALUTA, valuta);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_IMPORTO_VALPRIN, importoValprin);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_DATA, data);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_DESCRIZIONE, descrizione);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_RIPETIZIONE_ID, ripetizione_id);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_CONTO, conto);
		modificaSpesa.putExtra(CostantiPubbliche.VOCE_FAVORITE, preferito);
		startActivityForResult(modificaSpesa, 0);

	}


	//AsyncTask per recuperare i dettagli delle spese ripetute.
	private class ImpostaDettagliSpesaRipetuta extends AsyncTask<Long, Object, Cursor> {

		protected Cursor doInBackground(Long... params) {
			dbcSpeseRipetute.openLettura();
			Cursor curSpesaRipetuta = dbcSpeseRipetute.getItemRepeated(params[0]);

			return curSpesaRipetuta;
		}

		protected void onPostExecute(Cursor curSpesaRipetuta) {
			curSpesaRipetuta.moveToFirst();
			String ripetizione = curSpesaRipetuta.getString(curSpesaRipetuta.getColumnIndex("ripetizione"));
			dataFine = curSpesaRipetuta.getLong(curSpesaRipetuta.getColumnIndex("data_fine"));
			dataInizio = curSpesaRipetuta.getLong(curSpesaRipetuta.getColumnIndex("data_inizio"));

			String tipiBudget[] = getResources().getStringArray(R.array.ripetizioni);
			if (ripetizione.equals("nessuna")) {
				ripetizione = tipiBudget[0];
			} else if (ripetizione.equals("giornaliero")) {
				ripetizione = tipiBudget[1];
			} else if (ripetizione.equals("settimanale")) {
				ripetizione = tipiBudget[2];
			} else if (ripetizione.equals("bisettimanale")) {
				ripetizione = tipiBudget[3];
			} else if (ripetizione.equals("mensile")) {
				ripetizione = tipiBudget[4];
			} else if (ripetizione.equals("annuale")) {
				ripetizione = tipiBudget[5];
			} else if (ripetizione.equals("giorni_lavorativi")) {
				ripetizione = tipiBudget[6];
			} else if (ripetizione.equals("weekend")) {
				ripetizione = tipiBudget[7];
			}

			tvRipetizione.setText(ripetizione);
			DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, miaLocale);
			tvFineRipetizione.setText(df.format(new Date(dataFine)));
			curSpesaRipetuta.close();
			dbcSpeseRipetute.close();
		}
	}


	//implementazione di EliminaVociRipetute.EliminaVociRipetuteListener: serve per specificare, per le voci ripetute, se eliminarne una o pi� di una
	@Override
	public void onDialogPositiveClick(int sceltaElimina) {
		confermaElimina = true;
		Long oggi = FunzioniComuni.getDataAttuale();

		switch (sceltaElimina) {
			case ELIMINA_SOLO_QUESTA:
				new EliminaQuestaSpesaTask().execute(id);
				new AggiornaTabellaBudgetTask(ESTRAI_BUDGET_PER_AGGIUNTA_ELIMINAZIONE_SPESA, "%" + tag + "%", Long.valueOf(data).toString(), Long.valueOf(data).toString()).execute((Object[]) null);
				break;
			case ELIMINA_TUTTE:
				new EliminaTutteSpeseRipetuteTask().execute(ripetizione_id);
				new AggiornaTabellaBudgetTask(ESTRAI_BUDGET_PER_ELIMINAZIONE_SPESE_RIPETUTE, "%" + tag + "%", Long.valueOf(dataInizio).toString(), Long.valueOf(dataInizio).toString(), Long.valueOf(dataInizio).toString(), oggi.toString(), oggi.toString(), oggi.toString(), Long.valueOf(dataInizio).toString(), oggi.toString()).execute((Object[]) null);
				break;
			case ELIMINA_DA_OGGI:
				new EliminaSpeseRipetuteDaOggiTask().execute(ripetizione_id);
				new AggiornaTabellaBudgetTask(ESTRAI_BUDGET_PER_AGGIUNTA_ELIMINAZIONE_SPESA, "%" + tag + "%", oggi.toString(), oggi.toString()).execute((Object[]) null);
				break;
		}
	}


	//AsyncTask per caricare la HashMap con i suoni dell'app
	private class CaricaSuoniTask extends AsyncTask<Object, Object, Boolean> {

		protected Boolean doInBackground(Object... params) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(SpeseDettaglioVoce.this);
			boolean abilitazioneSuoni = pref.getBoolean(CostantiPreferenze.SUONI_ABILITATI, false);
			if (abilitazioneSuoni) {
				soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
				mappaSuoni = new SparseIntArray(1);
				mappaSuoni.put(CostantiSuoni.SUONO_CANCELLAZIONE, soundPool.load(SpeseDettaglioVoce.this, R.raw.cancellazione, 1));
				mappaSuoni.put(CostantiSuoni.SUONO_AGGIUNGI_SPESA_ENTRATA, soundPool.load(SpeseDettaglioVoce.this, R.raw.spese_entrate_budget_aggiunta, 1));
			}

			return abilitazioneSuoni;
		}

		protected void onPostExecute(Boolean result) {
			//una volta caricati i suoni nella Map l'app � pronta ad utilizzarli, non prima
			suoniAbilitati = result;
		}
	}


	// Loading images (icon and header) in a separate thread.
	private class LoadImagesTask extends AsyncTask<String, Object, Bitmap> {
		@Override
		protected Bitmap doInBackground(String... params) {
			DBCSpeseVoci dbcSpeseVoci = new DBCSpeseVoci(SpeseDettaglioVoce.this);
			dbcSpeseVoci.openLettura();
			Cursor curVoci = dbcSpeseVoci.getTutteLeVociFiltrato(params[0]);

			int iconaId = R.drawable.tag_0;
			if (curVoci.moveToFirst()) {
				int icona = curVoci.getInt(curVoci.getColumnIndex("icona"));
				iconaId = ListViewIconeVeloce.arrIconeId[icona];
			}
			final int iconaIdDef = iconaId;

			curVoci.close();
			dbcSpeseVoci.close();

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					new LoadHeaderImageTask().execute(iconaIdDef);
				}
			});

            return ListViewIconeVeloce.decodeSampledBitmapFromResource(getResources(), iconaId, 90, 90);
		}

		@Override
		protected void onPostExecute(Bitmap miaBitmap) {
			ivIcona.setImageBitmap(miaBitmap);
			ivIconToolbar.setImageBitmap(miaBitmap);
		}
	}

	// Load the header image and blur it in a separate thread. Takes iconId as parameter.
	private class LoadHeaderImageTask extends AsyncTask<Integer, Object, Bitmap> {
		int backgroundColor = R.color.primary_light;

		@Override
		protected Bitmap doInBackground(Integer... params) {
			int iconaId = params[0];

			Bitmap origBitmap = ListViewIconeVeloce.decodeSampledBitmapFromResource(getResources(), iconaId, 128, 128);
			Bitmap blurredBitmap = BlurBuilder.blur(SpeseDettaglioVoce.this, origBitmap);

			// Get a suitable color for image background.
			Palette palette = Palette.from(origBitmap).generate();
			backgroundColor = palette.getMutedColor(ContextCompat.getColor(SpeseDettaglioVoce.this, R.color.primary_light));

			return blurredBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap miaBitmap) {
            ImageView ivHeader = (ImageView) findViewById(R.id.main_imageview_placeholder2);
            ivHeader.setImageBitmap(miaBitmap);
			ivHeader.setBackgroundColor(Color.rgb(Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor)));

            // Show the header image with a fadein-fadeout animation.
            ViewSwitcher viewSwitcher = (ViewSwitcher) findViewById(R.id.vsHeader);
            viewSwitcher.showNext();
        }
	}


	/*
		Duplicazione voce: AsyncTask x duplicare la voce in un thread separato. NB: duplicazione =
		aggiungere nuova voce con data oggi e con gli stessi dati.
	 */
	private class DuplicaVoce extends AsyncTask<Object, Void, Void> {
		@Override
		protected Void doInBackground(Object... params) {
			dbcSpeseSostenute.openModifica();
			dbcSpeseSostenute.inserisciSpesaSostenuta(FunzioniComuni.getDataAttuale(), tag, importo, valuta, importoValprin, descrizione, ripetizione_id, conto, 0);
			dbcSpeseSostenute.close();
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			String msg = getResources().getString(R.string.dettagli_voce_spesa_duplicata);
			new MioToast(SpeseDettaglioVoce.this, msg).visualizza(Toast.LENGTH_SHORT);

			setResult(Activity.RESULT_OK);
			finish();
		}
	}


	//AsyncTask per eliminare solo la spesa selezionata
	private class EliminaQuestaSpesaTask extends AsyncTask<Long, Object, Object> {
		@Override
		protected Object doInBackground(Long... params) {
			dbcSpeseSostenute.openModifica();
			dbcSpeseSostenute.eliminaSpesaSostenuta(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			dbcSpeseSostenute.close();

			String msg = getResources().getQuantityString(R.plurals.dettagli_voce_x_voci_eliminate, 1, 1);
			new MioToast(SpeseDettaglioVoce.this, msg).visualizza(Toast.LENGTH_SHORT);

			Intent intRitorno = new Intent();
			intRitorno.putExtra(TIPO_OPERAZIONE, OPERAZIONE_ELIMINAZIONE);
			setResult(Activity.RESULT_OK, intRitorno);
			finish();
		}
	}

	/*AsyncTask x eliminare tutte le spese ripetute. Elimina sia dalla tabella spese_sost che dalla
	 * tabella spese_ripet (se si eliminano tutte le ripetizioni il record in spese_ripet non serve pi�).
	 * Nel campo Long passo la ripetizione_id
	 */
	private class EliminaTutteSpeseRipetuteTask extends AsyncTask<Long, Object, Integer> {
		@Override
		protected Integer doInBackground(Long... params) {
			dbcSpeseSostenute.openModifica();
			int speseCancellate = dbcSpeseSostenute.eliminaSpeseRipetute(params[0]);

			dbcSpeseRipetute.openModifica();
			dbcSpeseRipetute.eliminaSpesaRipetuta(params[0]);

			return speseCancellate;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dbcSpeseSostenute.close();
			dbcSpeseRipetute.close();

			String msg = getResources().getQuantityString(R.plurals.dettagli_voce_x_voci_eliminate, result, result);
			new MioToast(SpeseDettaglioVoce.this, msg).visualizza(Toast.LENGTH_SHORT);

			Intent intRitorno = new Intent();
			intRitorno.putExtra(TIPO_OPERAZIONE, OPERAZIONE_ELIMINAZIONE);
			setResult(Activity.RESULT_OK, intRitorno);
			finish();
		}
	}

	/*AsyncTask x eliminare tutte le spese ripetute da oggi in poi. Elimina solo dalla tabella
	 * spese_sost.
	 * Nel campo Long passo la ripetizione_id
	 */
	private class EliminaSpeseRipetuteDaOggiTask extends AsyncTask<Long, Object, Integer> {
		@Override
		protected Integer doInBackground(Long... params) {
			GregorianCalendar oggi = new GregorianCalendar();
			int giorno = oggi.get(GregorianCalendar.DATE);
			int mese = oggi.get(GregorianCalendar.MONTH);
			int anno = oggi.get(GregorianCalendar.YEAR);
			oggi = new GregorianCalendar(anno, mese, giorno);

			dbcSpeseSostenute.openModifica();
			int speseCancellate = dbcSpeseSostenute.eliminaSpeseRipetuteDallaData(params[0], oggi.getTimeInMillis());

			dbcSpeseRipetute.openModifica();
			Cursor curSpeseRipetute = dbcSpeseRipetute.getItemRepeated(params[0]);
			curSpeseRipetute.moveToFirst();
			dbcSpeseRipetute.aggiornaSpesaRipetuta(params[0], curSpeseRipetute.getString(curSpeseRipetute.getColumnIndex("voce")), curSpeseRipetute.getString(curSpeseRipetute.getColumnIndex("ripetizione")), curSpeseRipetute.getDouble(curSpeseRipetute.getColumnIndex("importo")), curSpeseRipetute.getString(curSpeseRipetute.getColumnIndex("valuta")), curSpeseRipetute.getDouble(curSpeseRipetute.getColumnIndex("importo_valprin")), curSpeseRipetute.getString(curSpeseRipetute.getColumnIndex("descrizione")), curSpeseRipetute.getLong(curSpeseRipetute.getColumnIndex("data_inizio")), 1, oggi.getTimeInMillis(), oggi.getTimeInMillis(), curSpeseRipetute.getString(curSpeseRipetute.getColumnIndex("conto")));
			curSpeseRipetute.close();

			return speseCancellate;
		}

		@Override
		protected void onPostExecute(Integer result) {
			dbcSpeseSostenute.close();
			dbcSpeseRipetute.close();

			String msg = getResources().getQuantityString(R.plurals.dettagli_voce_x_voci_eliminate, result, result);
			new MioToast(SpeseDettaglioVoce.this, msg).visualizza(Toast.LENGTH_SHORT);

			Intent intRitorno = new Intent();
			intRitorno.putExtra(TIPO_OPERAZIONE, OPERAZIONE_ELIMINAZIONE);
			setResult(Activity.RESULT_OK, intRitorno);
			finish();
		}
	}

	//ritorno dall'Activity SpeseAggiungi per modificare la spesa
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			Intent intRitorno = new Intent();
			intRitorno.putExtra(TIPO_OPERAZIONE, OPERAZIONE_MODIFICA);
			setResult(Activity.RESULT_OK, intRitorno);
			finish();
		}
	}

	//AsyncTask per aggiornare la tabella spese_budget campo spesa_sost a seguito della eliminazione della/e spesa/e
	private class AggiornaTabellaBudgetTask extends AsyncTask<Object, Object, Boolean> {
		String query;
		String args[];

		public AggiornaTabellaBudgetTask(String query, String... args) {
			this.query = query;
			this.args = args;
		}

		protected Boolean doInBackground(Object... params) {
			FunzioniAggiornamento aggBudget = new FunzioniAggiornamento(SpeseDettaglioVoce.this);
			int budgetAggiornati = aggBudget.aggiornaTabBudgetSpeseSost(query, args);

			return budgetAggiornati != -1;
		}

		protected void onPostExecute(Boolean result) {
			if (!result) {
				new MioToast(SpeseDettaglioVoce.this, getString(R.string.toast_aggiornamentoDatabase_errore)).visualizza(Toast.LENGTH_SHORT);
			}

			final Intent intAggiornaWidget = new Intent(WIDGET_AGGIORNA);
			sendBroadcast(intAggiornaWidget);
		}
	}

	// Animations related to AppBar.

	// Generic alpha animation.
	private void startAlphaAnimation(View view, long duration, int visibility) {
		AlphaAnimation alphaAnimation = (visibility == View.VISIBLE) ? new AlphaAnimation(0f, 1f) : new AlphaAnimation(1f, 0f);
		alphaAnimation.setDuration(duration);
		alphaAnimation.setFillAfter(true);
		view.startAnimation(alphaAnimation);
	}

	// Hide/show title in collapsed app bar.
	private void handleToolbarTitleVisibility(float percentage) {
		if(percentage == 1) {
			ivIconToolbar.setVisibility(View.VISIBLE);
			isIconToolbarVisible = true;
		}
		else if(isIconToolbarVisible) {
			ivIconToolbar.setVisibility(View.INVISIBLE);
			isIconToolbarVisible = false;
		}

		if(percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
			if (!isToolbarTitleVisible) {
				tvToolbarTitle.setVisibility(View.VISIBLE);
				startAlphaAnimation(tvToolbarTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
				isToolbarTitleVisible = true;
			}
		}
		else {
			if(isToolbarTitleVisible) {
				tvToolbarTitle.setVisibility(View.INVISIBLE);
				startAlphaAnimation(tvToolbarTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
				isToolbarTitleVisible = false;
			}
		}
	}

	// Hide/show title in expanded app bar.
	private void handleExpandedTitleVisibility(float percentage) {
		if(percentage <= PERCENTAGE_TO_HIDE_TITLE_DETAILS) {
			if (!isExpandedTitleVisible) {
				startAlphaAnimation(llExpandedTitle, ALPHA_ANIMATIONS_DURATION, View.VISIBLE);
				isExpandedTitleVisible = true;
			}
		}
		else {
			if(isExpandedTitleVisible) {
				startAlphaAnimation(llExpandedTitle, ALPHA_ANIMATIONS_DURATION, View.INVISIBLE);
				isExpandedTitleVisible = false;
			}
		}
	}



	//variabili di istanza
	private TextView tvVoce;
	private TextView tvImporto;
	private TextView tvConto;
	private TextView tvData;
	private TextView tvDescrizione;
	private TextView tvRipetizione;
	private TextView tvFineRipetizione;
	private ImageView ivIcona;
	private ImageView ivIconToolbar;
	private FloatingActionButton fabAlto;
	private TextView tvToolbarTitle;
	private LinearLayout llExpandedTitle;
	private long id;
	private long ripetizione_id;
	private double importo;
	private String conto;
	private int preferito;
	private String tag;
	private long data;
	private String descrizione;
	private long dataFine;
	private long dataInizio;
	private String valuta;
	private double importoValprin;
	private DBCSpeseSostenute dbcSpeseSostenute;
	private DBCSpeseRipetute dbcSpeseRipetute;
	private Currency currValuta;
	Locale miaLocale = (Locale.getDefault().getDisplayLanguage().equals("italiano") ? Locale.getDefault() : Locale.UK);
	private boolean isToolbarTitleVisible = false;
	private boolean isExpandedTitleVisible = true;
	private boolean isIconToolbarVisible = false;

	//gestione suoni
	private SoundPool soundPool;
	private SparseIntArray mappaSuoni;
	private boolean suoniAbilitati;
	private boolean confermaElimina;
	private boolean confermaDuplica;
}
