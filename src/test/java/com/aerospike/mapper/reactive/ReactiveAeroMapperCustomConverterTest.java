package com.aerospike.mapper.reactive;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.mapper.annotations.AerospikeKey;
import com.aerospike.mapper.annotations.AerospikeRecord;
import com.aerospike.mapper.annotations.FromAerospike;
import com.aerospike.mapper.annotations.ToAerospike;
import com.aerospike.mapper.tools.ReactiveAeroMapper;
import org.junit.Before;
import org.junit.Test;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReactiveAeroMapperCustomConverterTest extends ReactiveAeroMapperBaseTest {

    private ReactiveAeroMapper reactiveMapper;

    @Before
    public void setup() {
        reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient).build();
        reactorClient.getAerospikeClient().truncate(null, NAMESPACE, "poker", null);
    }

    public enum Suit {
        CLUBS, DIAMONDS, HEARTS, SPADES
    }

    @AerospikeRecord(namespace = NAMESPACE, set = "card")
    public static class Card {
        public char rank;
        public Suit suit;

        public Card() {}
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

    @AerospikeRecord(namespace = NAMESPACE, set = "poker")
    public static class PokerHand {
        public Card playerCard1;
        public Card playerCard2;
        public List<Card> tableCards;
        @AerospikeKey
        public String id;

        public PokerHand(String id, Card playerCard1, Card playerCard2, List<Card> tableCards) {
            super();
            this.playerCard1 = playerCard1;
            this.playerCard2 = playerCard2;
            this.tableCards = tableCards;
            this.id = id;
        }

        public PokerHand() {
        }

        @Override
        public boolean equals(Object obj) {
            PokerHand hand = (PokerHand) obj;
            if (!this.playerCard1.equals(hand.playerCard1)) {
                return false;
            }
            if (!this.playerCard2.equals(hand.playerCard2)) {
                return false;
            }
            if (this.tableCards.size() != hand.tableCards.size()) {
                return false;
            }
            for (int i = 0; i < this.tableCards.size(); i++) {
                if (!this.tableCards.get(i).equals(hand.tableCards.get(i))) {
                    return false;
                }
            }
            return true;
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
                case 'C': return new Card(rank, Suit.CLUBS);
                case 'D': return new Card(rank, Suit.DIAMONDS);
                case 'H': return new Card(rank, Suit.HEARTS);
                case 'S': return new Card(rank, Suit.SPADES);
                default:
                    throw new AerospikeException("unknown suit: " + card);
            }
        }
    }

    @Test
    public void testSave() {
        PokerHand blackjackHand = new PokerHand(
                "1",
                new Card('6', Suit.SPADES),
                new Card('9', Suit.HEARTS),
                Arrays.asList(new Card('4', Suit.CLUBS), new Card('A', Suit.HEARTS)));

        reactiveMapper = new ReactiveAeroMapper.Builder(reactorClient)
                .addConverter(new CardConverter())
                .build();

        reactiveMapper.save(blackjackHand).subscribeOn(Schedulers.parallel()).block();

        PokerHand hand2 = reactiveMapper.read(blackjackHand.getClass(), blackjackHand.id).subscribeOn(Schedulers.parallel()).block();
        assertEquals(blackjackHand, hand2);

        KeyRecord keyRecord = reactorClient.get(null, new Key(NAMESPACE, "poker", "1")).subscribeOn(Schedulers.parallel()).block();
        assert keyRecord != null;
        assertEquals("6S", keyRecord.record.getString("playerCard1"));
        assertEquals("9H", keyRecord.record.getString("playerCard2"));
        assertEquals(2, keyRecord.record.getList("tableCards").size());
        assertEquals("4C", keyRecord.record.getList("tableCards").get(0));
        assertEquals("AH", keyRecord.record.getList("tableCards").get(1));
    }
}
