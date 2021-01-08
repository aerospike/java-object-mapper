package com.aerospike.mapper;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.FromAerospike;
import com.aerospike.mapper.annotations.ToAerospike;
import com.aerospike.mapper.tools.AeroMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AeroMapperCustomConverterTest extends AeroMapperBaseTest {

    private AeroMapper mapper;

    @Before
    public void setup() {
        mapper = new AeroMapper.Builder(client).build();
        client.truncate(null, NAMESPACE, "testSet", null);
    }

    public static enum Suit {
        CLUBS, DIAMONDS, HEARTS, SPADES;
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "blackjack", mapAll = true)
    public static class BlackjackHand {
        public Card hidden_card;
        public List<Card> visible_cards;
        @AerospikeKey
        public String id;

        public BlackjackHand(String id, Card hidden_card, List<Card> visible_cards) {
            super();
            this.id = id;
            this.hidden_card = hidden_card;
            this.visible_cards = visible_cards;
        }

        public BlackjackHand() {
        }

        @Override
        public boolean equals(Object obj) {
            BlackjackHand hand = (BlackjackHand) obj;
            if (!this.hidden_card.equals(hand.hidden_card)) {
                return false;
            }
            if (this.visible_cards.size() != hand.visible_cards.size()) {
                return false;
            }
            for (int i = 0; i < this.visible_cards.size(); i++) {
                if (!this.visible_cards.get(i).equals(hand.visible_cards.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class Card {
        public final char rank;
        public final Suit suit;

        public Card(char rank, Suit suit) {
            super();
            this.rank = rank;
            this.suit = suit;
        }

        @Override
        public boolean equals(Object obj) {
            Card card = (Card) obj;
            return this.rank == card.rank && this.suit == card.suit;
        }
    }

    public static class CardConverter {
        @ToAerospike
        public String toAerospike(Card card) {
            return card.rank + card.suit.name().substring(0, 1);
        }

        @FromAerospike
        public Card fromAerospike(String card) {
            if (card.length() != 2) throw new AerospikeException("Unknown card: " + card);

            char rank = card.charAt(0);
            switch (card.charAt(1)) {
                case 'C':
                    return new Card(rank, Suit.CLUBS);
                case 'D':
                    return new Card(rank, Suit.DIAMONDS);
                case 'H':
                    return new Card(rank, Suit.HEARTS);
                case 'S':
                    return new Card(rank, Suit.SPADES);
                default:
                    throw new AerospikeException("unknown suit: " + card);
            }

        }
    }

    @Test
    public void testSave() {
        BlackjackHand blackjackHand = new BlackjackHand(
                "1",
                new Card('6', Suit.SPADES),
                Arrays.asList(new Card('4', Suit.CLUBS), new Card('A', Suit.HEARTS)));
        mapper = new AeroMapper.Builder(client)
                .addConverter(new CardConverter())
                .build();

        mapper.save(blackjackHand);

        BlackjackHand hand2 = mapper.read(blackjackHand.getClass(), blackjackHand.id);
        assertEquals(blackjackHand, hand2);

        Record record = client.get(null, new Key(NAMESPACE, "blackjack", "1"));
        assertEquals("6S", record.getString("hidden_card"));
        assertEquals("4C", record.getList("visible_cards").get(0));
        assertEquals("AH", record.getList("visible_cards").get(1));
    }
}
