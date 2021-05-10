/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.examples.UseCase6_GroupedMultiSelection;

import static org.eclipse.keyple.core.service.examples.common.ConfigurationUtil.CONTACTLESS_READER_NAME_REGEX;
import static org.eclipse.keyple.core.service.examples.common.ConfigurationUtil.getCardReader;

import java.util.Map;
import org.eclipse.keyple.card.generic.GenericExtensionService;
import org.eclipse.keyple.card.generic.GenericExtensionServiceProvider;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.examples.common.ConfigurationUtil;
import org.eclipse.keyple.core.service.selection.CardSelectionResult;
import org.eclipse.keyple.core.service.selection.CardSelectionService;
import org.eclipse.keyple.core.service.selection.CardSelector;
import org.eclipse.keyple.core.service.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.service.selection.spi.SmartCard;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 * <h1>Use Case ‘generic 6’ – Grouped selections based on an AID prefix (PC/SC)</h1>
 *
 * <p>We demonstrate here the selection of two applications in a single card, with both applications
 * selected using the same AID and the "FIRST" and "NEXT" navigation options but grouped in the same
 * selection process.<br>
 * Both selection results are available in the {@link CardSelectionResult} object returned by the
 * execution of the selection scenario.
 *
 * <h2>Scenario:</h2>
 *
 * <ul>
 *   <li>Check if a ISO 14443-4 card is in the reader, select a card (here a card having two
 *       applications whose DF Names are prefixed by a specific AID [see AID_KEYPLE_PREFIX]).
 *   <li>Run a double AID based application selection scenario (first and next occurrence).
 *   <li>Output collected of all smart cards data (FCI and ATR).
 * </ul>
 *
 * All results are logged with slf4j.
 *
 * <p>Any unexpected behavior will result in runtime exceptions.
 *
 * @since 2.0
 */
public class Main_GroupedMultiSelection_Pcsc {
  private static final Logger logger =
      LoggerFactory.getLogger(Main_GroupedMultiSelection_Pcsc.class);

  public static void main(String[] args) {

    // Get the instance of the SmartCardService (singleton pattern)
    SmartCardService smartCardService = SmartCardServiceProvider.getService();

    // Register the PcscPlugin with the SmartCardService, get the corresponding generic plugin in
    // return.
    Plugin plugin = smartCardService.registerPlugin(PcscPluginFactoryBuilder.builder().build());

    Reader reader = getCardReader(plugin, CONTACTLESS_READER_NAME_REGEX);
    // Get the generic card extension service
    GenericExtensionService cardExtension = GenericExtensionServiceProvider.getService();

    // Verify that the extension's API level is consistent with the current service.
    smartCardService.checkCardExtension(cardExtension);

    logger.info(
        "=============== UseCase Generic #6: Grouped selections based on an AID prefix ==================");

    // Check if a card is present in the reader
    if (!reader.isCardPresent()) {
      throw new IllegalStateException("No card is present in the reader.");
    }

    logger.info("= #### Select application with AID = '{}'.", ConfigurationUtil.AID_KEYPLE_PREFIX);

    // Get the core card selection service.
    CardSelectionService selectionService =
        CardSelectionServiceFactory.getService(MultiSelectionProcessing.PROCESS_ALL);

    // First selection: get the first application occurrence matching the AID, keep the
    // physical channel open
    // Prepare the selection by adding the created generic selection to the card selection scenario.
    selectionService.prepareSelection(
        cardExtension.createCardSelection(
            CardSelector.builder()
                .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
                .setFileOccurrence(CardSelector.FileOccurrence.FIRST)
                .build()));

    // Second selection: get the next application occurrence matching the same AID, close the
    // physical channel after
    // Prepare the selection by adding the created generic selection to the card selection scenario.
    selectionService.prepareSelection(
        cardExtension.createCardSelection(
            CardSelector.builder()
                .filterByDfName(ConfigurationUtil.AID_KEYPLE_PREFIX)
                .setFileOccurrence(CardSelector.FileOccurrence.NEXT)
                .build()));

    // close the channel after the selection
    selectionService.prepareReleaseChannel();

    CardSelectionResult cardSelectionsResult =
        selectionService.processCardSelectionScenario(reader);

    // log the result
    for (Map.Entry<Integer, SmartCard> entry : cardSelectionsResult.getSmartCards().entrySet()) {
      SmartCard smartCard = entry.getValue();
      String atr = smartCard.hasAtr() ? ByteArrayUtil.toHex(smartCard.getAtrBytes()) : "no ATR";
      String fci = smartCard.hasFci() ? ByteArrayUtil.toHex(smartCard.getFciBytes()) : "no FCI";
      String selectionIsActive =
          smartCard == cardSelectionsResult.getActiveSmartCard() ? "true" : "false";
      logger.info(
          "Selection status for selection (indexed {}): \n\t\tActive smart card: {}\n\t\tATR: {}\n\t\tFCI: {}",
          entry.getKey(),
          selectionIsActive,
          atr,
          fci);
    }

    logger.info("= #### End of the generic card processing.");

    System.exit(0);
  }
}
